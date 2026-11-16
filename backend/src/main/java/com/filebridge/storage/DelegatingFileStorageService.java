package com.filebridge.storage;

import com.filebridge.exception.FileNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * Primary FileStorageService that routes storage operations between
 * Supabase Storage (cloud) and LocalFileStorageService (local disk).
 *
 * Guarantees zero downtime and seamless backward compatibility for existing local files
 * while directing new uploads to the private Supabase bucket.
 */
@Primary
@Service("fileStorageService")
public class DelegatingFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(DelegatingFileStorageService.class);

    private final LocalFileStorageService localStorageService;
    private final SupabaseStorageService supabaseStorageService;
    private final String preferredStorageType;

    public DelegatingFileStorageService(
            LocalFileStorageService localStorageService,
            SupabaseStorageService supabaseStorageService,
            @Value("${filebridge.storage.type:SUPABASE}") String preferredStorageType) {
        this.localStorageService = localStorageService;
        this.supabaseStorageService = supabaseStorageService;
        this.preferredStorageType = preferredStorageType;
    }

    /**
     * Determines whether Supabase Storage is active and ready for uploads.
     */
    public boolean isSupabaseActive() {
        return "SUPABASE".equalsIgnoreCase(preferredStorageType) && supabaseStorageService.isConfigured();
    }

    @Override
    public void store(InputStream inputStream, String storageKey, String contentType, long sizeBytes) {
        if (isSupabaseKey(storageKey) || isSupabaseActive()) {
            supabaseStorageService.store(inputStream, storageKey, contentType, sizeBytes);
        } else {
            localStorageService.store(inputStream, storageKey, contentType, sizeBytes);
        }
    }

    @Override
    public InputStream retrieve(String storageKey) {
        if (isSupabaseKey(storageKey)) {
            return supabaseStorageService.retrieve(storageKey);
        }
        if (localStorageService.exists(storageKey)) {
            return localStorageService.retrieve(storageKey);
        }
        if (supabaseStorageService.isConfigured()) {
            return supabaseStorageService.retrieve(storageKey);
        }
        throw new FileNotFoundException("File not found in storage: " + storageKey);
    }

    @Override
    public void delete(String storageKey) {
        if (isSupabaseKey(storageKey)) {
            supabaseStorageService.delete(storageKey);
            return;
        }
        if (localStorageService.exists(storageKey)) {
            localStorageService.delete(storageKey);
            return;
        }
        if (supabaseStorageService.isConfigured()) {
            supabaseStorageService.delete(storageKey);
        }
    }

    /**
     * Delete multiple files from cloud/local storage.
     */
    public void deleteMultiple(List<String> storageKeys) {
        if (storageKeys == null || storageKeys.isEmpty()) {
            return;
        }
        List<String> supabaseKeys = storageKeys.stream().filter(this::isSupabaseKey).toList();
        List<String> localKeys = storageKeys.stream().filter(k -> !isSupabaseKey(k)).toList();

        if (!supabaseKeys.isEmpty()) {
            supabaseStorageService.deleteMultiple(supabaseKeys);
        }
        for (String localKey : localKeys) {
            try {
                if (localStorageService.exists(localKey)) {
                    localStorageService.delete(localKey);
                }
            } catch (Exception e) {
                log.warn("Failed to delete local storage file '{}': {}", localKey, e.getMessage());
            }
        }
    }

    @Override
    public boolean exists(String storageKey) {
        if (isSupabaseKey(storageKey)) {
            return supabaseStorageService.exists(storageKey);
        }
        if (localStorageService.exists(storageKey)) {
            return true;
        }
        if (supabaseStorageService.isConfigured()) {
            return supabaseStorageService.exists(storageKey);
        }
        return false;
    }

    /**
     * Checks if a storage key belongs to Supabase (e.g. users/{userId}/{fileId}).
     */
    public boolean isSupabaseKey(String key) {
        if (key == null) return false;
        String normalized = key.startsWith("/") ? key.substring(1) : key;
        return normalized.startsWith("users/");
    }

    public LocalFileStorageService getLocalStorageService() {
        return localStorageService;
    }

    public SupabaseStorageService getSupabaseStorageService() {
        return supabaseStorageService;
    }
}
