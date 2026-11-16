package com.filebridge.service;

import com.filebridge.dto.FolderRequest;
import com.filebridge.dto.FolderResponse;
import com.filebridge.dto.RenameRequest;
import com.filebridge.exception.FileNotFoundException;
import com.filebridge.model.FileMetadata;
import com.filebridge.model.Folder;
import com.filebridge.repository.FileMetadataRepository;
import com.filebridge.repository.FolderRepository;
import com.filebridge.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FolderService {

    private static final Logger log = LoggerFactory.getLogger(FolderService.class);

    private final FolderRepository folderRepository;
    private final FileMetadataRepository fileRepository;
    private final FileStorageService storageService;

    public FolderService(FolderRepository folderRepository,
                         FileMetadataRepository fileRepository,
                         FileStorageService storageService) {
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
        this.storageService = storageService;
    }

    /**
     * Create a new folder under a parent (or root).
     */
    public FolderResponse createFolder(FolderRequest request, String ownerId) {
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be empty.");
        }
        String folderName = request.getName().trim();
        String parentId = normalizeParentId(request.getParentId());

        if (parentId != null) {
            Folder parent = folderRepository.findByIdAndOwnerId(parentId, ownerId)
                    .orElseThrow(() -> new FileNotFoundException("Parent folder not found with id: " + parentId));
            if (parent.isInTrash()) {
                throw new IllegalArgumentException("Cannot create folder inside a deleted folder.");
            }
        }

        // Check for duplicate name in same parent
        boolean exists = parentId != null
                ? folderRepository.existsByOwnerIdAndParentIdAndNameIgnoreCaseAndInTrashFalse(ownerId, parentId, folderName)
                : folderRepository.existsByOwnerIdAndParentIdIsNullAndNameIgnoreCaseAndInTrashFalse(ownerId, folderName);

        if (exists) {
            throw new IllegalArgumentException("A folder named '" + folderName + "' already exists in this location.");
        }

        Instant now = Instant.now();
        Folder folder = Folder.builder()
                .name(folderName)
                .parentId(parentId)
                .ownerId(ownerId)
                .createdAt(now)
                .updatedAt(now)
                .inTrash(false)
                .build();

        Folder saved = folderRepository.save(folder);
        log.info("Created folder '{}' ({}) for owner '{}'", folderName, saved.getId(), ownerId);
        return toFolderResponse(saved);
    }

    /**
     * List folders in a given parent directory (or root if parentId is null).
     */
    public List<FolderResponse> listFolders(String ownerId, String parentId) {
        String normalizedParentId = normalizeParentId(parentId);
        List<Folder> folders = normalizedParentId != null
                ? folderRepository.findByOwnerIdAndParentIdAndInTrashFalse(ownerId, normalizedParentId)
                : folderRepository.findByOwnerIdAndParentIdIsNullAndInTrashFalse(ownerId);

        return folders.stream()
                .map(this::toFolderResponse)
                .sorted(Comparator.comparing(FolderResponse::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    /**
     * Get folder metadata by ID.
     */
    public FolderResponse getFolder(String id, String ownerId) {
        Folder folder = findOwnedFolder(id, ownerId);
        return toFolderResponse(folder);
    }

    /**
     * Rename a folder.
     */
    public FolderResponse renameFolder(String id, RenameRequest request, String ownerId) {
        if (request == null || request.getNewName() == null || request.getNewName().trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be empty.");
        }
        String newName = request.getNewName().trim();
        Folder folder = findOwnedFolder(id, ownerId);

        if (folder.getName().equalsIgnoreCase(newName)) {
            return toFolderResponse(folder);
        }

        // Check duplicate name in same parent
        boolean exists = folder.getParentId() != null
                ? folderRepository.existsByOwnerIdAndParentIdAndNameIgnoreCaseAndInTrashFalse(ownerId, folder.getParentId(), newName)
                : folderRepository.existsByOwnerIdAndParentIdIsNullAndNameIgnoreCaseAndInTrashFalse(ownerId, newName);

        if (exists) {
            throw new IllegalArgumentException("A folder named '" + newName + "' already exists in this location.");
        }

        folder.setName(newName);
        folder.setUpdatedAt(Instant.now());
        Folder updated = folderRepository.save(folder);
        log.info("Renamed folder '{}' to '{}' for owner '{}'", id, newName, ownerId);
        return toFolderResponse(updated);
    }

    /**
     * Move a folder and all its contents to trash (soft delete).
     */
    public void moveFolderToTrash(String id, String ownerId) {
        Folder folder = findOwnedFolder(id, ownerId);
        Instant now = Instant.now();

        Set<String> allFolderIds = collectFolderAndDescendants(folder, ownerId);

        // Mark all descendant folders as in trash
        for (String fId : allFolderIds) {
            folderRepository.findByIdAndOwnerId(fId, ownerId).ifPresent(f -> {
                f.setInTrash(true);
                f.setDeletedAt(now);
                folderRepository.save(f);
            });
        }

        // Mark all files in these folders as in trash
        for (String fId : allFolderIds) {
            List<FileMetadata> filesInFolder = fileRepository.findByOwnerIdAndFolderIdAndInTrashFalse(ownerId, fId);
            for (FileMetadata file : filesInFolder) {
                file.setInTrash(true);
                file.setDeletedAt(now);
                fileRepository.save(file);
            }
        }

        log.info("Moved folder '{}' and {} subfolder(s) to trash for owner '{}'", id, allFolderIds.size() - 1, ownerId);
    }

    /**
     * Restore a folder and its contents from trash.
     */
    public void restoreFolder(String id, String ownerId) {
        Folder folder = findOwnedFolder(id, ownerId);
        if (!folder.isInTrash()) {
            return;
        }

        // If parent folder is in trash or missing, reset parentId to null (root)
        if (folder.getParentId() != null) {
            Optional<Folder> parentOpt = folderRepository.findByIdAndOwnerId(folder.getParentId(), ownerId);
            if (parentOpt.isEmpty() || parentOpt.get().isInTrash()) {
                folder.setParentId(null);
            }
        }

        folder.setInTrash(false);
        folder.setDeletedAt(null);
        folder.setUpdatedAt(Instant.now());
        folderRepository.save(folder);

        // Also restore contained files and subfolders
        Set<String> allFolderIds = collectFolderAndDescendants(folder, ownerId);
        for (String fId : allFolderIds) {
            folderRepository.findByIdAndOwnerId(fId, ownerId).ifPresent(f -> {
                f.setInTrash(false);
                f.setDeletedAt(null);
                folderRepository.save(f);
            });

            List<FileMetadata> files = fileRepository.findByFolderId(fId);
            for (FileMetadata file : files) {
                if (file.getOwnerId().equals(ownerId) && file.isInTrash()) {
                    file.setInTrash(false);
                    file.setDeletedAt(null);
                    fileRepository.save(file);
                }
            }
        }

        log.info("Restored folder '{}' and its contents for owner '{}'", id, ownerId);
    }

    /**
     * Permanently delete a folder and all its contents from disk and database.
     */
    public void permanentlyDeleteFolder(String id, String ownerId) {
        Folder folder = findOwnedFolder(id, ownerId);
        Set<String> allFolderIds = collectFolderAndDescendants(folder, ownerId);

        // Delete all files in these folders from storage and database
        for (String fId : allFolderIds) {
            List<FileMetadata> files = fileRepository.findByFolderId(fId);
            for (FileMetadata file : files) {
                if (file.getOwnerId().equals(ownerId)) {
                    try {
                        storageService.delete(file.getStorageKey());
                    } catch (Exception e) {
                        log.warn("Failed to delete storage key '{}' for file '{}': {}",
                                file.getStorageKey(), file.getId(), e.getMessage());
                    }
                    fileRepository.delete(file);
                }
            }
        }

        // Delete all folder records
        for (String fId : allFolderIds) {
            folderRepository.deleteByIdAndOwnerId(fId, ownerId);
        }

        log.info("Permanently deleted folder '{}' and {} descendant folders for owner '{}'",
                id, allFolderIds.size() - 1, ownerId);
    }

    /**
     * Retrieve the list of folders currently in trash for owner.
     */
    public List<FolderResponse> listTrashFolders(String ownerId) {
        return folderRepository.findByOwnerIdAndInTrashTrue(ownerId).stream()
                .map(this::toFolderResponse)
                .sorted(Comparator.comparing(FolderResponse::getDeletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Helper to get full breadcrumb lineage from root to folder.
     */
    public List<FolderResponse> getFolderLineage(String folderId, String ownerId) {
        List<FolderResponse> lineage = new ArrayList<>();
        String currentId = folderId;
        Set<String> visited = new HashSet<>();

        while (currentId != null && !visited.contains(currentId)) {
            visited.add(currentId);
            Optional<Folder> fOpt = folderRepository.findByIdAndOwnerId(currentId, ownerId);
            if (fOpt.isEmpty()) break;
            Folder f = fOpt.get();
            lineage.add(0, toFolderResponse(f));
            currentId = f.getParentId();
        }

        return lineage;
    }

    private Folder findOwnedFolder(String id, String ownerId) {
        return folderRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new FileNotFoundException("Folder not found with id: " + id));
    }

    private Set<String> collectFolderAndDescendants(Folder rootFolder, String ownerId) {
        Set<String> result = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(rootFolder.getId());
        result.add(rootFolder.getId());

        while (!queue.isEmpty()) {
            String parentId = queue.poll();
            List<Folder> subfolders = folderRepository.findByParentId(parentId);
            for (Folder sub : subfolders) {
                if (sub.getOwnerId().equals(ownerId) && result.add(sub.getId())) {
                    queue.add(sub.getId());
                }
            }
        }
        return result;
    }

    private String normalizeParentId(String parentId) {
        return (parentId == null || parentId.trim().isEmpty() || "null".equalsIgnoreCase(parentId.trim()) || "root".equalsIgnoreCase(parentId.trim()))
                ? null
                : parentId.trim();
    }

    private FolderResponse toFolderResponse(Folder folder) {
        int fileCount = (int) fileRepository.findByOwnerIdAndFolderIdAndInTrashFalse(folder.getOwnerId(), folder.getId()).size();
        int subfolderCount = (int) folderRepository.findByOwnerIdAndParentIdAndInTrashFalse(folder.getOwnerId(), folder.getId()).size();

        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParentId())
                .ownerId(folder.getOwnerId())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .inTrash(folder.isInTrash())
                .deletedAt(folder.getDeletedAt())
                .fileCount(fileCount)
                .subfolderCount(subfolderCount)
                .build();
    }
}
