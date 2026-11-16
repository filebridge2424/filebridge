package com.filebridge.storage;

import com.filebridge.exception.FileStorageException;
import com.filebridge.storage.supabase.SupabaseClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Dedicated Supabase Storage implementation of FileStorageService.
 * Interacts with private Supabase Storage buckets via authenticated SupabaseClient.
 */
@Service("supabaseStorageService")
public class SupabaseStorageService implements FileStorageService {

    private final SupabaseClient supabaseClient;

    @Autowired
    public SupabaseStorageService(SupabaseClient supabaseClient) {
        this.supabaseClient = supabaseClient;
    }

    /**
     * Checks if Supabase credentials are configured and ready for operations.
     */
    public boolean isConfigured() {
        return supabaseClient != null && supabaseClient.isConfigured();
    }

    @Override
    public void store(InputStream inputStream, String storageKey, String contentType, long sizeBytes) {
        byte[] payload;
        try {
            payload = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new FileStorageException("Failed to read input stream for upload: " + storageKey, e);
        }
        supabaseClient.upload(storageKey, contentType, payload);
    }

    @Override
    public InputStream retrieve(String storageKey) {
        return supabaseClient.download(storageKey);
    }

    @Override
    public void delete(String storageKey) {
        supabaseClient.delete(Collections.singletonList(storageKey));
    }

    /**
     * Bulk delete files from Supabase bucket in a single batch request.
     */
    public void deleteMultiple(List<String> storageKeys) {
        supabaseClient.delete(storageKeys);
    }

    @Override
    public boolean exists(String storageKey) {
        return supabaseClient.exists(storageKey);
    }

    public SupabaseClient getSupabaseClient() {
        return supabaseClient;
    }
}
