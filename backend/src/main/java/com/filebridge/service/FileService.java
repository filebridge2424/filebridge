package com.filebridge.service;

import com.filebridge.dto.FileMetadataResponse;
import com.filebridge.dto.StorageUsageResponse;
import com.filebridge.exception.AccessDeniedException;
import com.filebridge.exception.FileNotFoundException;
import com.filebridge.exception.FileStorageException;
import com.filebridge.exception.StorageQuotaExceededException;
import com.filebridge.model.*;
import com.filebridge.repository.FileMetadataRepository;
import com.filebridge.repository.FileShareRepository;
import com.filebridge.repository.FolderRepository;
import com.filebridge.repository.UserRepository;
import com.filebridge.storage.DelegatingFileStorageService;
import com.filebridge.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileMetadataRepository repository;
    private final FolderRepository folderRepository;
    private final FileStorageService storageService;
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final FileShareRepository shareRepository;
    private final long maxStorageBytes;

    @Autowired
    public FileService(
            FileMetadataRepository repository,
            FolderRepository folderRepository,
            FileStorageService storageService,
            SubscriptionService subscriptionService,
            UserRepository userRepository,
            ActivityService activityService,
            FileShareRepository shareRepository,
            @Value("${filebridge.max-storage-bytes:10737418240}") long maxStorageBytes) {
        this.repository = repository;
        this.folderRepository = folderRepository;
        this.storageService = storageService;
        this.subscriptionService = subscriptionService;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.shareRepository = shareRepository;
        this.maxStorageBytes = maxStorageBytes;
    }

    /**
     * Backward-compatible uploadFile without folderId.
     */
    public FileMetadataResponse uploadFile(MultipartFile file, String ownerId) {
        return uploadFile(file, null, ownerId);
    }

    /**
     * Upload a file associated with the authenticated user and optional folder.
     * Enforces storage quota before uploading.
     * Stores bytes in Supabase Storage (when configured) or local storage as fallback.
     */
    public FileMetadataResponse uploadFile(MultipartFile file, String folderId, String ownerId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty.");
        }
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("User identifier is required.");
        }

        // Storage Quota Enforcement
        long userLimit = maxStorageBytes;
        if (subscriptionService != null && userRepository != null) {
            User owner = userRepository.findById(ownerId).orElse(null);
            if (owner != null) {
                userLimit = subscriptionService.getStorageLimitForUser(owner);
            }
        }

        List<FileMetadata> existingFiles = repository.findByOwnerId(ownerId);
        long currentUsedBytes = existingFiles.stream().mapToLong(FileMetadata::getSizeBytes).sum();

        if (currentUsedBytes + file.getSize() > userLimit) {
            long limitMb = userLimit / (1024 * 1024);
            long fileMb = Math.max(1, file.getSize() / (1024 * 1024));
            long usedMb = currentUsedBytes / (1024 * 1024);
            throw new StorageQuotaExceededException(String.format(
                    "Storage quota exceeded! Your current plan allows %d MB (used: %d MB). This upload (%d MB) would exceed your available storage. Please delete files or upgrade to Premium.",
                    limitMb, usedMb, fileMb));
        }

        String normalizedFolderId = normalizeFolderId(folderId);
        if (normalizedFolderId != null) {
            Folder folder = folderRepository.findByIdAndOwnerId(normalizedFolderId, ownerId)
                    .orElseThrow(() -> new FileNotFoundException("Target folder not found with id: " + normalizedFolderId));
            if (folder.isInTrash()) {
                throw new IllegalArgumentException("Cannot upload to a folder that is in trash.");
            }
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String contentType = resolveContentType(file.getContentType(), originalFilename);

        boolean useSupabase = (storageService instanceof DelegatingFileStorageService delegating)
                && delegating.isSupabaseActive();

        String storageKey;
        String storageType;

        if (useSupabase) {
            storageType = "SUPABASE";
            String uniqueFileId = UUID.randomUUID().toString();
            String ext = extractExtension(originalFilename);
            storageKey = "users/" + ownerId + "/" + uniqueFileId + (ext.isEmpty() ? "" : "." + ext);
        } else {
            storageType = "LOCAL";
            storageKey = generateStorageKey(originalFilename);
        }

        try (InputStream inputStream = file.getInputStream()) {
            storageService.store(inputStream, storageKey, contentType, file.getSize());
        } catch (IOException e) {
            throw new FileStorageException("Failed to read uploaded file stream.", e);
        }

        Instant now = Instant.now();
        FileMetadata metadata = FileMetadata.builder()
                .originalFilename(originalFilename)
                .storageKey(storageKey)
                .storageType(storageType)
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .uploadedAt(now)
                .updatedAt(now)
                .ownerId(ownerId)
                .folderId(normalizedFolderId)
                .inTrash(false)
                .build();

        FileMetadata saved;
        try {
            saved = repository.save(metadata);
        } catch (Exception e) {
            // Avoid orphaned object in storage if DB fails
            try {
                storageService.delete(storageKey);
            } catch (Exception ex) {
                log.warn("Failed to delete orphaned storage key '{}': {}", storageKey, ex.getMessage());
            }
            throw e;
        }

        if (activityService != null) {
            activityService.logActivity(ownerId, ActivityType.FILE_UPLOADED, saved.getId(), originalFilename,
                    "Uploaded " + originalFilename + " (" + formatBytes(file.getSize()) + ")");
        }

        log.info("Uploaded file '{}' (id: {}, storage: {}) in folder '{}' for owner '{}'",
                originalFilename, saved.getId(), storageType, normalizedFolderId, ownerId);
        return toResponse(saved);
    }

    /**
     * List all non-trash files for owner (backward compatible).
     */
    public List<FileMetadataResponse> listFiles(String ownerId) {
        return repository.findByOwnerIdAndInTrashFalse(ownerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * List files with filtering by folder, search query, and sorting.
     */
    public List<FileMetadataResponse> listFiles(String ownerId, String folderId, String search,
                                                String sortBy, String sortDir, Boolean inTrashOnly) {
        List<FileMetadata> files;

        if (Boolean.TRUE.equals(inTrashOnly)) {
            files = repository.findByOwnerIdAndInTrashTrue(ownerId);
        } else if (folderId != null) {
            String normFolderId = normalizeFolderId(folderId);
            if (normFolderId != null) {
                files = repository.findByOwnerIdAndFolderIdAndInTrashFalse(ownerId, normFolderId);
            } else {
                files = repository.findByOwnerIdAndFolderIdIsNullAndInTrashFalse(ownerId);
            }
        } else {
            files = repository.findByOwnerIdAndInTrashFalse(ownerId);
        }

        // Search filtering (filename or contentType)
        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            files = files.stream()
                    .filter(f -> (f.getOriginalFilename() != null && f.getOriginalFilename().toLowerCase().contains(q)) ||
                                 (f.getContentType() != null && f.getContentType().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        // Sorting
        Comparator<FileMetadata> comparator = resolveComparator(sortBy, sortDir);
        files.sort(comparator);

        return files.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Rename an existing file. Only updates metadata, no re-upload of bytes needed.
     */
    public FileMetadataResponse renameFile(String id, String newName, String ownerId) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("New file name cannot be empty.");
        }
        FileMetadata metadata = findOwnedFile(id, ownerId);
        String oldName = metadata.getOriginalFilename();
        String sanitized = sanitizeFilename(newName.trim());

        // Preserve original extension if user did not include any extension
        String oldExt = extractExtension(metadata.getOriginalFilename());
        String newExt = extractExtension(sanitized);
        if (newExt.isEmpty() && !oldExt.isEmpty()) {
            sanitized = sanitized + "." + oldExt;
        }

        metadata.setOriginalFilename(sanitized);
        metadata.setUpdatedAt(Instant.now());
        FileMetadata updated = repository.save(metadata);

        if (activityService != null) {
            activityService.logActivity(ownerId, ActivityType.FILE_RENAMED, id, sanitized,
                    "Renamed '" + oldName + "' to '" + sanitized + "'");
        }

        log.info("Renamed file '{}' to '{}' for owner '{}'", id, sanitized, ownerId);
        return toResponse(updated);
    }

    /**
     * Move a file to another folder (or root if targetFolderId is null/empty).
     * Only updates metadata without moving physical bytes.
     */
    public FileMetadataResponse moveFile(String id, String targetFolderId, String ownerId) {
        FileMetadata metadata = findOwnedFile(id, ownerId);
        String normTarget = normalizeFolderId(targetFolderId);

        String folderName = "root";
        if (normTarget != null) {
            Folder target = folderRepository.findByIdAndOwnerId(normTarget, ownerId)
                    .orElseThrow(() -> new FileNotFoundException("Target folder not found with id: " + normTarget));
            if (target.isInTrash()) {
                throw new IllegalArgumentException("Cannot move file to a folder in trash.");
            }
            folderName = target.getName();
        }

        metadata.setFolderId(normTarget);
        metadata.setUpdatedAt(Instant.now());
        FileMetadata updated = repository.save(metadata);

        if (activityService != null) {
            activityService.logActivity(ownerId, ActivityType.FILE_MOVED, id, metadata.getOriginalFilename(),
                    "Moved '" + metadata.getOriginalFilename() + "' to " + folderName);
        }

        log.info("Moved file '{}' to folder '{}' for owner '{}'", id, normTarget, ownerId);
        return toResponse(updated);
    }

    /**
     * Move a file to trash (soft delete).
     */
    public void moveFileToTrash(String id, String ownerId) {
        FileMetadata metadata = findOwnedFile(id, ownerId);
        metadata.setInTrash(true);
        metadata.setDeletedAt(Instant.now());
        repository.save(metadata);

        if (activityService != null) {
            activityService.logActivity(ownerId, ActivityType.FILE_DELETED, id, metadata.getOriginalFilename(),
                    "Moved '" + metadata.getOriginalFilename() + "' to Trash");
        }

        log.info("Moved file '{}' ({}) to trash for owner '{}'", metadata.getOriginalFilename(), id, ownerId);
    }

    /**
     * Restore a file from trash.
     */
    public void restoreFile(String id, String ownerId) {
        FileMetadata metadata = findOwnedFile(id, ownerId);
        if (!metadata.isInTrash()) {
            return;
        }

        // If folder is deleted or missing, move restored file to root
        if (metadata.getFolderId() != null) {
            Optional<Folder> fOpt = folderRepository.findByIdAndOwnerId(metadata.getFolderId(), ownerId);
            if (fOpt.isEmpty() || fOpt.get().isInTrash()) {
                metadata.setFolderId(null);
            }
        }

        metadata.setInTrash(false);
        metadata.setDeletedAt(null);
        metadata.setUpdatedAt(Instant.now());
        repository.save(metadata);

        if (activityService != null) {
            activityService.logActivity(ownerId, ActivityType.FILE_RESTORED, id, metadata.getOriginalFilename(),
                    "Restored '" + metadata.getOriginalFilename() + "' from Trash");
        }

        log.info("Restored file '{}' ({}) for owner '{}'", metadata.getOriginalFilename(), id, ownerId);
    }

    /**
     * Permanently delete a file from storage (Supabase / local) and database.
     * Cleans up any sharing records associated with this file.
     */
    public void deleteFilePermanently(String id, String ownerId) {
        FileMetadata metadata = findOwnedFile(id, ownerId);
        try {
            storageService.delete(metadata.getStorageKey());
        } catch (Exception e) {
            log.warn("Could not delete physical storage key '{}' for file '{}': {}",
                    metadata.getStorageKey(), id, e.getMessage());
        }

        // Clean up any shares for this file
        if (shareRepository != null) {
            try {
                shareRepository.deleteByFileId(id);
            } catch (Exception e) {
                log.warn("Could not delete shares for file '{}': {}", id, e.getMessage());
            }
        }

        repository.delete(metadata);

        if (activityService != null) {
            activityService.logActivity(ownerId, ActivityType.FILE_PERMANENTLY_DELETED, id, metadata.getOriginalFilename(),
                    "Permanently deleted '" + metadata.getOriginalFilename() + "'");
        }

        log.info("Permanently deleted file '{}' ({}) for owner '{}'", metadata.getOriginalFilename(), id, ownerId);
    }

    /**
     * Delete file either soft or permanently.
     */
    public void deleteFile(String id, String ownerId, boolean permanent) {
        FileMetadata metadata = findOwnedFile(id, ownerId);
        if (permanent || metadata.isInTrash()) {
            deleteFilePermanently(id, ownerId);
        } else {
            moveFileToTrash(id, ownerId);
        }
    }

    /**
     * Backward-compatible deleteFile: defaults to soft-delete if not already in trash.
     */
    public void deleteFile(String id, String ownerId) {
        deleteFile(id, ownerId, false);
    }

    /**
     * Get recent active files for the user.
     */
    public List<FileMetadataResponse> getRecentFiles(String ownerId, int limit) {
        return repository.findByOwnerIdAndInTrashFalse(ownerId)
                .stream()
                .sorted(Comparator.comparing(FileMetadata::getUploadedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get metadata for a file only if owned by the authenticated user.
     */
    public FileMetadataResponse getFileMetadata(String id, String ownerId) {
        return toResponse(findOwnedFile(id, ownerId));
    }

    /**
     * Get metadata for a file if owned or if shared with user.
     */
    public FileMetadataResponse getFileMetadataAccessible(String id, String userId, String userEmail) {
        FileMetadata metadata = findAccessibleFile(id, userId, userEmail, false);
        return toResponse(metadata);
    }

    /**
     * Open an InputStream for the file bytes, verifying ownership first (backward compatible).
     */
    public InputStream downloadFile(String id, String ownerId) {
        return downloadFileAccessible(id, ownerId, null, false);
    }

    /**
     * Open an InputStream for the file bytes, allowing access if owner OR shared with permission.
     */
    public InputStream downloadFileAccessible(String id, String userId, String userEmail, boolean isPreview) {
        FileMetadata metadata = findAccessibleFile(id, userId, userEmail, !isPreview);
        if (!storageService.exists(metadata.getStorageKey())) {
            throw new FileNotFoundException("File bytes not found in storage for id: " + id);
        }

        if (activityService != null && !isPreview) {
            activityService.logActivity(userId, ActivityType.FILE_DOWNLOADED, id, metadata.getOriginalFilename(),
                    "Downloaded '" + metadata.getOriginalFilename() + "'");
        }

        return storageService.retrieve(metadata.getStorageKey());
    }

    /**
     * Get raw entity for building headers, verifying ownership first.
     */
    public FileMetadata getFileEntity(String id, String ownerId) {
        return findOwnedFile(id, ownerId);
    }

    /**
     * Get raw entity for building headers, verifying ownership or share access.
     */
    public FileMetadata getFileEntityAccessible(String id, String userId, String userEmail, boolean requireDownload) {
        return findAccessibleFile(id, userId, userEmail, requireDownload);
    }

    /**
     * Calculate storage usage for the authenticated user, reflecting their actual Free / Premium plan limit.
     */
    public StorageUsageResponse getStorageUsage(String ownerId) {
        User user = userRepository != null ? userRepository.findById(ownerId).orElse(null) : null;
        long limitBytes = (subscriptionService != null && user != null)
                ? subscriptionService.getStorageLimitForUser(user)
                : maxStorageBytes;

        List<FileMetadata> userFiles = repository.findByOwnerId(ownerId);
        long usedBytes = userFiles.stream().mapToLong(FileMetadata::getSizeBytes).sum();
        long trashBytes = userFiles.stream().filter(FileMetadata::isInTrash).mapToLong(FileMetadata::getSizeBytes).sum();
        long activeCount = userFiles.stream().filter(f -> !f.isInTrash()).count();

        long remainingBytes = Math.max(0, limitBytes - usedBytes);
        double usedPercent = limitBytes > 0
                ? Math.min(100.0, (double) usedBytes / limitBytes * 100.0)
                : 0.0;

        return StorageUsageResponse.builder()
                .usedBytes(usedBytes)
                .limitBytes(limitBytes)
                .usedPercent(Math.round(usedPercent * 100.0) / 100.0)
                .remainingBytes(remainingBytes)
                .fileCount(activeCount)
                .trashBytes(trashBytes)
                .plan(user != null ? user.getSubscriptionPlan() : SubscriptionPlan.FREE)
                .subscriptionStatus(user != null ? user.getSubscriptionStatus() : SubscriptionStatus.ACTIVE)
                .subscriptionEndDate(user != null ? user.getSubscriptionEndDate() : null)
                .build();
    }

    private FileMetadata findOwnedFile(String id, String ownerId) {
        return repository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new FileNotFoundException("File not found with id: " + id));
    }

    private FileMetadata findAccessibleFile(String id, String userId, String userEmail, boolean requireDownloadPermission) {
        FileMetadata metadata = repository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("File not found with id: " + id));

        // If user is owner, grant access
        if (metadata.getOwnerId().equals(userId)) {
            return metadata;
        }

        // If not owner, check sharing records
        if (shareRepository != null) {
            List<FileShare> shares = shareRepository.findByFileIdAndRevokedFalse(id);
            for (FileShare s : shares) {
                if (s.isActive()) {
                    boolean userMatches = (userId != null && userId.equals(s.getSharedWithUserId())) ||
                            (userEmail != null && userEmail.equalsIgnoreCase(s.getSharedWithEmail()));

                    if (userMatches) {
                        if (requireDownloadPermission && s.getPermission() != SharePermission.DOWNLOAD) {
                            throw new AccessDeniedException("You only have VIEW permission for this shared file. Download is restricted.");
                        }
                        return metadata;
                    }
                }
            }
        }

        throw new AccessDeniedException("You do not have permission to access this file.");
    }

    private String generateStorageKey(String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        String extension = extractExtension(originalFilename);
        return extension.isEmpty() ? uuid : uuid + "." + extension;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        String ext = filename.substring(filename.lastIndexOf('.') + 1);
        return ext.matches("[a-zA-Z0-9]{1,10}") ? ext : "";
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "unnamed";
        return Paths.get(filename).getFileName().toString();
    }

    private String resolveContentType(String contentType, String filename) {
        if (contentType != null && !contentType.isBlank() && !contentType.equals("application/octet-stream")) {
            return contentType;
        }
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".gif")) return "image/gif";
            if (lower.endsWith(".webp")) return "image/webp";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".pdf")) return "application/pdf";
            if (lower.endsWith(".txt")) return "text/plain";
            if (lower.endsWith(".json")) return "application/json";
        }
        return (contentType != null && !contentType.isBlank()) ? contentType : "application/octet-stream";
    }

    private String normalizeFolderId(String folderId) {
        return (folderId == null || folderId.trim().isEmpty() || "null".equalsIgnoreCase(folderId.trim()) || "root".equalsIgnoreCase(folderId.trim()))
                ? null
                : folderId.trim();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private Comparator<FileMetadata> resolveComparator(String sortBy, String sortDir) {
        boolean asc = "asc".equalsIgnoreCase(sortDir);
        Comparator<FileMetadata> comp;

        if ("size".equalsIgnoreCase(sortBy) || "sizeBytes".equalsIgnoreCase(sortBy)) {
            comp = Comparator.comparingLong(FileMetadata::getSizeBytes);
        } else if ("name".equalsIgnoreCase(sortBy) || "originalFilename".equalsIgnoreCase(sortBy)) {
            comp = Comparator.comparing(f -> f.getOriginalFilename() != null ? f.getOriginalFilename().toLowerCase() : "");
        } else { // default uploadedAt
            comp = Comparator.comparing(FileMetadata::getUploadedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        }

        return asc ? comp : comp.reversed();
    }

    private FileMetadataResponse toResponse(FileMetadata metadata) {
        return FileMetadataResponse.builder()
                .id(metadata.getId())
                .originalFilename(metadata.getOriginalFilename())
                .storageType(metadata.getStorageType())
                .contentType(metadata.getContentType())
                .sizeBytes(metadata.getSizeBytes())
                .uploadedAt(metadata.getUploadedAt())
                .updatedAt(metadata.getUpdatedAt())
                .ownerId(metadata.getOwnerId())
                .folderId(metadata.getFolderId())
                .inTrash(metadata.isInTrash())
                .deletedAt(metadata.getDeletedAt())
                .build();
    }
}
