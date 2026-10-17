package com.filebridge.storage;

import com.filebridge.exception.FileStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Local filesystem implementation of FileStorageService.
 * Files are stored in a configurable base directory using UUID-based storage keys.
 * Path traversal attacks are prevented by resolving and validating all paths.
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path basePath;

    public LocalFileStorageService(@Value("${filebridge.storage.local.base-path}") String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
        initializeStorageDirectory();
    }

    private void initializeStorageDirectory() {
        try {
            Files.createDirectories(basePath);
            log.info("Storage directory initialized at: {}", basePath);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage directory: " + basePath, e);
        }
    }

    @Override
    public void store(InputStream inputStream, String storageKey, String contentType, long sizeBytes) {
        Path target = resolveAndValidate(storageKey);
        try {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored file with key '{}' at '{}'", storageKey, target);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file with key: " + storageKey, e);
        }
    }

    @Override
    public InputStream retrieve(String storageKey) {
        Path target = resolveAndValidate(storageKey);
        if (!Files.exists(target)) {
            throw new FileStorageException("File not found in storage: " + storageKey);
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read file with key: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        Path target = resolveAndValidate(storageKey);
        try {
            boolean deleted = Files.deleteIfExists(target);
            if (deleted) {
                log.debug("Deleted file with key '{}'", storageKey);
            } else {
                log.warn("File not found for deletion: '{}'", storageKey);
            }
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file with key: " + storageKey, e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        Path target = resolveAndValidate(storageKey);
        return Files.exists(target);
    }

    /**
     * Resolves a storage key to an absolute path and validates it stays within basePath.
     * This prevents path traversal attacks.
     */
    private Path resolveAndValidate(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key must not be blank");
        }
        Path resolved = basePath.resolve(storageKey).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new FileStorageException("Invalid storage key — path traversal detected: " + storageKey);
        }
        return resolved;
    }
}
