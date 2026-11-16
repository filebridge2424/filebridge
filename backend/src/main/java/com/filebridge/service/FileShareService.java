package com.filebridge.service;

import com.filebridge.dto.ShareRequest;
import com.filebridge.dto.ShareResponse;
import com.filebridge.dto.SharedFileResponse;
import com.filebridge.exception.AccessDeniedException;
import com.filebridge.exception.FileNotFoundException;
import com.filebridge.exception.ShareAccessDeniedException;
import com.filebridge.model.*;
import com.filebridge.repository.FileMetadataRepository;
import com.filebridge.repository.FileShareRepository;
import com.filebridge.repository.UserRepository;
import com.filebridge.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileShareService {

    private static final Logger log = LoggerFactory.getLogger(FileShareService.class);

    private final FileShareRepository shareRepository;
    private final FileMetadataRepository fileRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final FileStorageService storageService;

    public FileShareService(
            FileShareRepository shareRepository,
            FileMetadataRepository fileRepository,
            UserRepository userRepository,
            ActivityService activityService,
            FileStorageService storageService) {
        this.shareRepository = shareRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.storageService = storageService;
    }

    /**
     * Share a file with a recipient email OR generate a shareable link.
     */
    public ShareResponse shareFile(ShareRequest request, String ownerId) {
        if (request == null || request.getFileId() == null || request.getFileId().isBlank()) {
            throw new IllegalArgumentException("File ID is required for sharing.");
        }

        FileMetadata file = fileRepository.findByIdAndOwnerId(request.getFileId(), ownerId)
                .orElseThrow(() -> new FileNotFoundException("File not found or not owned by you."));

        if (file.isInTrash()) {
            throw new IllegalArgumentException("Cannot share a file that is in Trash.");
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new FileNotFoundException("Owner user not found."));

        SharePermission permission = request.getPermission() != null ? request.getPermission() : SharePermission.VIEW;

        // Case 1: Public / Shareable link
        if (request.isLinkShare() || (request.getRecipientEmail() == null || request.getRecipientEmail().isBlank())) {
            Optional<FileShare> existingLink = shareRepository.findByFileIdAndIsLinkShareTrueAndRevokedFalse(file.getId());
            if (existingLink.isPresent()) {
                FileShare linkShare = existingLink.get();
                linkShare.setPermission(permission);
                linkShare.setExpiresAt(request.getExpiresAt());
                FileShare saved = shareRepository.save(linkShare);
                return toShareResponse(saved, file);
            }

            FileShare newLinkShare = FileShare.builder()
                    .fileId(file.getId())
                    .ownerId(ownerId)
                    .ownerEmail(owner.getEmail())
                    .ownerName(owner.getName())
                    .permission(permission)
                    .shareToken(UUID.randomUUID().toString())
                    .isLinkShare(true)
                    .createdAt(Instant.now())
                    .expiresAt(request.getExpiresAt())
                    .revoked(false)
                    .build();

            FileShare saved = shareRepository.save(newLinkShare);
            activityService.logActivity(ownerId, ActivityType.FILE_SHARED, file.getId(), file.getOriginalFilename(),
                    "Created share link with " + permission + " permission for " + file.getOriginalFilename());
            log.info("Created share link for file '{}' by owner '{}'", file.getId(), ownerId);
            return toShareResponse(saved, file);
        }

        // Case 2: Share with specific registered user via email
        String recipientEmail = request.getRecipientEmail().trim().toLowerCase();
        if (recipientEmail.equalsIgnoreCase(owner.getEmail())) {
            throw new IllegalArgumentException("You cannot share a file with yourself.");
        }

        User recipientUser = userRepository.findByEmailIgnoreCase(recipientEmail)
                .orElseThrow(() -> new IllegalArgumentException("No registered user found with email: " + recipientEmail));

        // Check if already shared
        Optional<FileShare> existingShare = shareRepository.findByFileIdAndSharedWithEmailIgnoreCaseAndRevokedFalse(file.getId(), recipientEmail);
        FileShare share;
        if (existingShare.isPresent()) {
            share = existingShare.get();
            share.setPermission(permission);
            share.setExpiresAt(request.getExpiresAt());
            share = shareRepository.save(share);
        } else {
            share = FileShare.builder()
                    .fileId(file.getId())
                    .ownerId(ownerId)
                    .ownerEmail(owner.getEmail())
                    .ownerName(owner.getName())
                    .sharedWithUserId(recipientUser.getId())
                    .sharedWithEmail(recipientEmail)
                    .permission(permission)
                    .shareToken(UUID.randomUUID().toString())
                    .isLinkShare(false)
                    .createdAt(Instant.now())
                    .expiresAt(request.getExpiresAt())
                    .revoked(false)
                    .build();
            share = shareRepository.save(share);
        }

        activityService.logActivity(ownerId, ActivityType.FILE_SHARED, file.getId(), file.getOriginalFilename(),
                "Shared file '" + file.getOriginalFilename() + "' with " + recipientEmail + " (" + permission + ")");
        activityService.logActivity(recipientUser.getId(), ActivityType.FILE_SHARED, file.getId(), file.getOriginalFilename(),
                owner.getName() + " shared '" + file.getOriginalFilename() + "' with you (" + permission + ")");

        log.info("Shared file '{}' with '{}' ({})", file.getId(), recipientEmail, permission);
        return toShareResponse(share, file);
    }

    /**
     * Get all active shares for a specific file (owner only).
     */
    public List<ShareResponse> getFileShares(String fileId, String ownerId) {
        FileMetadata file = fileRepository.findByIdAndOwnerId(fileId, ownerId)
                .orElseThrow(() -> new FileNotFoundException("File not found or not owned by you."));

        return shareRepository.findByFileIdAndRevokedFalse(file.getId())
                .stream()
                .map(share -> toShareResponse(share, file))
                .collect(Collectors.toList());
    }

    /**
     * Revoke a share (owner only).
     */
    public void revokeShare(String shareId, String ownerId) {
        FileShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new FileNotFoundException("Share not found with id: " + shareId));

        if (!share.getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("You do not have permission to revoke this share.");
        }

        share.setRevoked(true);
        shareRepository.save(share);

        FileMetadata file = fileRepository.findById(share.getFileId()).orElse(null);
        String fileName = file != null ? file.getOriginalFilename() : "file";

        activityService.logActivity(ownerId, ActivityType.SHARE_REVOKED, share.getFileId(), fileName,
                "Revoked sharing access for " + (share.isLinkShare() ? "link share" : share.getSharedWithEmail()));

        if (share.getSharedWithUserId() != null) {
            activityService.logActivity(share.getSharedWithUserId(), ActivityType.SHARE_REVOKED, share.getFileId(), fileName,
                    "Access to '" + fileName + "' was revoked by owner");
        }

        log.info("Revoked share '{}' for file '{}' by owner '{}'", shareId, share.getFileId(), ownerId);
    }

    /**
     * Get all files shared with the current authenticated user.
     */
    public List<SharedFileResponse> getSharedWithMe(String userId, String userEmail) {
        List<FileShare> shares = new ArrayList<>();

        if (userId != null && !userId.isBlank()) {
            shares.addAll(shareRepository.findBySharedWithUserIdAndRevokedFalse(userId));
        }
        if (userEmail != null && !userEmail.isBlank()) {
            List<FileShare> emailShares = shareRepository.findBySharedWithEmailIgnoreCaseAndRevokedFalse(userEmail.trim());
            for (FileShare s : emailShares) {
                if (shares.stream().noneMatch(existing -> existing.getId().equals(s.getId()))) {
                    shares.add(s);
                }
            }
        }

        List<SharedFileResponse> result = new ArrayList<>();
        for (FileShare share : shares) {
            if (share.isExpired() || share.isRevoked()) {
                continue;
            }
            Optional<FileMetadata> fileOpt = fileRepository.findById(share.getFileId());
            if (fileOpt.isPresent()) {
                FileMetadata file = fileOpt.get();
                // If file is in trash or permanently deleted, do not show in shared with me
                if (!file.isInTrash()) {
                    result.add(new SharedFileResponse(
                            share.getId(),
                            file.getId(),
                            file.getOriginalFilename(),
                            file.getContentType(),
                            file.getSizeBytes(),
                            share.getOwnerName(),
                            share.getOwnerEmail(),
                            share.getPermission(),
                            share.getCreatedAt(),
                            share.getExpiresAt()
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Validates and returns active FileShare by token.
     */
    public FileShare getValidShareByToken(String token) {
        FileShare share = shareRepository.findByShareTokenAndRevokedFalse(token)
                .orElseThrow(() -> new ShareAccessDeniedException("Invalid, expired, or revoked share link."));

        if (share.isExpired()) {
            throw new ShareAccessDeniedException("This share link has expired.");
        }

        return share;
    }

    /**
     * Returns public share metadata for valid token.
     */
    public ShareResponse getPublicShareMetadata(String token) {
        FileShare share = getValidShareByToken(token);
        FileMetadata file = fileRepository.findById(share.getFileId())
                .orElseThrow(() -> new FileNotFoundException("Shared file not found."));
        if (file.isInTrash()) {
            throw new FileNotFoundException("Shared file has been moved to Trash or deleted.");
        }
        return toShareResponse(share, file);
    }

    /**
     * Stream public file content by token.
     */
    public InputStream getPublicShareContent(String token, boolean requireDownload) {
        FileShare share = getValidShareByToken(token);
        if (requireDownload && share.getPermission() != SharePermission.DOWNLOAD) {
            throw new ShareAccessDeniedException("Download permission is not granted for this share link.");
        }
        FileMetadata file = fileRepository.findById(share.getFileId())
                .orElseThrow(() -> new FileNotFoundException("Shared file not found."));
        if (file.isInTrash()) {
            throw new FileNotFoundException("Shared file has been moved to Trash or deleted.");
        }
        return storageService.retrieve(file.getStorageKey());
    }

    public FileMetadata getPublicShareFile(String token) {
        FileShare share = getValidShareByToken(token);
        FileMetadata file = fileRepository.findById(share.getFileId())
                .orElseThrow(() -> new FileNotFoundException("Shared file not found."));
        if (file.isInTrash()) {
            throw new FileNotFoundException("Shared file has been moved to Trash or deleted.");
        }
        return file;
    }

    /**
     * Stream shared file content for authenticated user.
     */
    public InputStream getSharedFileContent(String shareId, String userId, String userEmail, boolean requireDownload) {
        FileShare share = getValidShareForUser(shareId, userId, userEmail);
        if (requireDownload && share.getPermission() != SharePermission.DOWNLOAD) {
            throw new ShareAccessDeniedException("You only have VIEW permission for this shared file.");
        }
        FileMetadata file = fileRepository.findById(share.getFileId())
                .orElseThrow(() -> new FileNotFoundException("Shared file not found."));
        if (file.isInTrash()) {
            throw new FileNotFoundException("Shared file has been moved to Trash or deleted.");
        }

        if (activityService != null && requireDownload) {
            activityService.logActivity(userId, ActivityType.FILE_DOWNLOADED, file.getId(), file.getOriginalFilename(),
                    "Downloaded shared file '" + file.getOriginalFilename() + "'");
        }

        return storageService.retrieve(file.getStorageKey());
    }

    public FileMetadata getSharedFile(String shareId, String userId, String userEmail) {
        FileShare share = getValidShareForUser(shareId, userId, userEmail);
        FileMetadata file = fileRepository.findById(share.getFileId())
                .orElseThrow(() -> new FileNotFoundException("Shared file not found."));
        if (file.isInTrash()) {
            throw new FileNotFoundException("Shared file has been moved to Trash or deleted.");
        }
        return file;
    }

    /**
     * Validates and returns active FileShare by share ID for authenticated user.
     */
    public FileShare getValidShareForUser(String shareId, String userId, String userEmail) {
        FileShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new FileNotFoundException("Share not found."));

        if (share.isRevoked() || share.isExpired()) {
            throw new ShareAccessDeniedException("This share access has expired or was revoked.");
        }

        boolean matchesUser = (userId != null && userId.equals(share.getSharedWithUserId())) ||
                (userEmail != null && userEmail.equalsIgnoreCase(share.getSharedWithEmail()));

        if (!matchesUser && !userId.equals(share.getOwnerId())) {
            throw new AccessDeniedException("You do not have permission to access this shared file.");
        }

        return share;
    }

    /**
     * Check if user has permission to download a file (either owner, or shared with DOWNLOAD permission).
     */
    public boolean canUserDownloadFile(String fileId, String userId, String userEmail) {
        Optional<FileMetadata> fileOpt = fileRepository.findById(fileId);
        if (fileOpt.isEmpty()) return false;
        FileMetadata file = fileOpt.get();
        if (file.getOwnerId().equals(userId)) return true;

        List<FileShare> shares = shareRepository.findByFileIdAndRevokedFalse(fileId);
        for (FileShare s : shares) {
            if (s.isActive()) {
                boolean matches = (userId != null && userId.equals(s.getSharedWithUserId())) ||
                        (userEmail != null && userEmail.equalsIgnoreCase(s.getSharedWithEmail()));
                if (matches && s.getPermission() == SharePermission.DOWNLOAD) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if user has permission to view a file (either owner, or shared with VIEW or DOWNLOAD permission).
     */
    public boolean canUserViewFile(String fileId, String userId, String userEmail) {
        Optional<FileMetadata> fileOpt = fileRepository.findById(fileId);
        if (fileOpt.isEmpty()) return false;
        FileMetadata file = fileOpt.get();
        if (file.getOwnerId().equals(userId)) return true;

        List<FileShare> shares = shareRepository.findByFileIdAndRevokedFalse(fileId);
        for (FileShare s : shares) {
            if (s.isActive()) {
                boolean matches = (userId != null && userId.equals(s.getSharedWithUserId())) ||
                        (userEmail != null && userEmail.equalsIgnoreCase(s.getSharedWithEmail()));
                if (matches) {
                    return true;
                }
            }
        }
        return false;
    }

    public FileShare getShareById(String shareId) {
        return shareRepository.findById(shareId)
                .orElseThrow(() -> new FileNotFoundException("Share not found with id: " + shareId));
    }

    public void removeSharesForFile(String fileId) {
        shareRepository.deleteByFileId(fileId);
    }

    private ShareResponse toShareResponse(FileShare share, FileMetadata file) {
        ShareResponse res = new ShareResponse();
        res.setId(share.getId());
        res.setFileId(share.getFileId());
        res.setFileName(file != null ? file.getOriginalFilename() : "file");
        res.setFileContentType(file != null ? file.getContentType() : "application/octet-stream");
        res.setFileSize(file != null ? file.getSizeBytes() : 0);
        res.setOwnerId(share.getOwnerId());
        res.setOwnerEmail(share.getOwnerEmail());
        res.setOwnerName(share.getOwnerName());
        res.setSharedWithUserId(share.getSharedWithUserId());
        res.setSharedWithEmail(share.getSharedWithEmail());
        res.setPermission(share.getPermission());
        res.setShareToken(share.getShareToken());
        res.setShareLink(share.getShareToken() != null ? "/share/" + share.getShareToken() : null);
        res.setLinkShare(share.isLinkShare());
        res.setCreatedAt(share.getCreatedAt());
        res.setExpiresAt(share.getExpiresAt());
        res.setRevoked(share.isRevoked());
        return res;
    }
}
