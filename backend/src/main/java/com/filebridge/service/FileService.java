package com.filebridge.service;

import com.filebridge.dto.FileMetadataResponse;
import com.filebridge.dto.StorageUsageResponse;
import com.filebridge.exception.FileNotFoundException;
import com.filebridge.exception.FileStorageException;
import com.filebridge.model.FileMetadata;
import com.filebridge.repository.FileMetadataRepository;
import com.filebridge.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileMetadataRepository repository;
    private final FileStorageService storageService;
    private final long maxStorageBytes;

    public FileService(
            FileMetadataRepository repository,
            FileStorageService storageService,
            @Value("${filebridge.max-storage-bytes:10737418240}") long maxStorageBytes) {
        this.repository = repository;
        this.storageService = storageService;
        this.maxStorageBytes = maxStorageBytes;
    }

    /**
     * Upload a file associated with the authenticated user.
     */
    public FileMetadataResponse uploadFile(MultipartFile file, String ownerId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty.");
        }
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("User identifier is required.");
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String contentType = resolveContentType(file.getContentType());
        String storageKey = generateStorageKey(originalFilename);

        try (InputStream inputStream = file.getInputStream()) {
            storageService.store(inputStream, storageKey, contentType, file.getSize());
        } catch (IOException e) {
            throw new FileStorageException("Failed to read uploaded file stream.", e);
        }

        FileMetadata metadata = FileMetadata.builder()
            .originalFilename(originalFilename)
            .storageKey(storageKey)
            .contentType(contentType)
            .sizeBytes(file.getSize())
            .uploadedAt(Instant.now())
            .ownerId(ownerId)
            .build();

        FileMetadata saved = repository.save(metadata);
        log.info("Uploaded file '{}' with id '{}' for owner '{}'",
            originalFilename, saved.getId(), ownerId);
        return toResponse(saved);
    }

    /**
     * List only files owned by the authenticated user.
     */
    public List<FileMetadataResponse> listFiles(String ownerId) {
        return repository.findByOwnerId(ownerId)
            .stream()
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
     * Open an InputStream for the file bytes, verifying ownership first.
     */
    public InputStream downloadFile(String id, String ownerId) {
        FileMetadata metadata = findOwnedFile(id, ownerId);
        if (!storageService.exists(metadata.getStorageKey())) {
            throw new FileNotFoundException("File bytes not found in storage for id: " + id);
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
     * Delete a file: remove bytes from storage and remove metadata, verifying ownership first.
     */
    public void deleteFile(String id, String ownerId) {
        FileMetadata metadata = findOwnedFile(id, ownerId);
        storageService.delete(metadata.getStorageKey());
        repository.delete(metadata);
        log.info("Deleted file '{}' with id '{}' for owner '{}'", metadata.getOriginalFilename(), id, ownerId);
    }

    /**
     * Calculate storage usage only for the authenticated user.
     */
    public StorageUsageResponse getStorageUsage(String ownerId) {
        List<FileMetadata> userFiles = repository.findByOwnerId(ownerId);
        long usedBytes = userFiles.stream().mapToLong(FileMetadata::getSizeBytes).sum();
        double usedPercent = maxStorageBytes > 0
            ? Math.min(100.0, (double) usedBytes / maxStorageBytes * 100.0)
            : 0.0;

        return StorageUsageResponse.builder()
            .usedBytes(usedBytes)
            .limitBytes(maxStorageBytes)
            .usedPercent(Math.round(usedPercent * 100.0) / 100.0)
            .build();
    }

    private FileMetadata findOwnedFile(String id, String ownerId) {
        // Enforce ownership: query by both id and ownerId. If missing or owned by another user, return 404
        return repository.findByIdAndOwnerId(id, ownerId)
            .orElseThrow(() -> new FileNotFoundException("File not found with id: " + id));
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

    private String resolveContentType(String contentType) {
        return (contentType != null && !contentType.isBlank())
            ? contentType
            : "application/octet-stream";
    }

    private FileMetadataResponse toResponse(FileMetadata metadata) {
        return FileMetadataResponse.builder()
            .id(metadata.getId())
            .originalFilename(metadata.getOriginalFilename())
            .contentType(metadata.getContentType())
            .sizeBytes(metadata.getSizeBytes())
            .uploadedAt(metadata.getUploadedAt())
            .ownerId(metadata.getOwnerId())
            .build();
    }
}
