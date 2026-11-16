package com.filebridge.storage;

import java.io.InputStream;

/**
 * Abstraction for file byte storage.
 * Implementations can be swapped (local filesystem, Supabase Storage, S3, etc.)
 * without changing controllers or service logic.
 */
public interface FileStorageService {

    /**
     * Store file bytes and return the storage key used to retrieve them.
     *
     * @param inputStream the file content stream
     * @param storageKey  the unique key under which to store the file
     * @param contentType MIME type hint (for remote implementations)
     * @param sizeBytes   file size in bytes
     */
    void store(InputStream inputStream, String storageKey, String contentType, long sizeBytes);

    /**
     * Retrieve file bytes by storage key.
     *
     * @param storageKey the storage key
     * @return InputStream of the file bytes
     */
    InputStream retrieve(String storageKey);

    /**
     * Delete file bytes by storage key.
     *
     * @param storageKey the storage key
     */
    void delete(String storageKey);

    /**
     * Check if a file exists in storage.
     *
     * @param storageKey the storage key
     * @return true if file exists
     */
    boolean exists(String storageKey);
}
