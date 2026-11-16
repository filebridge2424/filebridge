package com.filebridge.storage.supabase;

import java.io.InputStream;
import java.util.List;

/**
 * Interface representing direct interaction with the Supabase Storage REST API.
 */
public interface SupabaseClient {

    /**
     * Checks if Supabase credentials are configured.
     */
    boolean isConfigured();

    /**
     * Uploads bytes to the private Supabase bucket at the specified path.
     */
    void upload(String path, String contentType, byte[] data);

    /**
     * Downloads an InputStream from the private Supabase bucket at the specified path.
     */
    InputStream download(String path);

    /**
     * Deletes one or more files from the Supabase bucket.
     */
    void delete(List<String> paths);

    /**
     * Checks whether an object exists in the private Supabase bucket.
     */
    boolean exists(String path);

    String getSupabaseUrl();

    String getBucket();
}
