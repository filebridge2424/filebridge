package com.filebridge.storage.supabase;

import com.filebridge.exception.FileStorageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultSupabaseClientTest {

    @Test
    @DisplayName("isConfigured returns true with valid secret key")
    void testIsConfigured_Valid() {
        DefaultSupabaseClient client = new DefaultSupabaseClient(
                "https://yforzdoqhocjorkshzfx.supabase.co",
                "valid-service-role-secret-key-123",
                "filebridge-files"
        );
        assertTrue(client.isConfigured());
        assertEquals("https://yforzdoqhocjorkshzfx.supabase.co", client.getSupabaseUrl());
        assertEquals("filebridge-files", client.getBucket());
    }

    @Test
    @DisplayName("isConfigured returns false when key is empty or placeholder")
    void testIsConfigured_Invalid() {
        DefaultSupabaseClient emptyClient = new DefaultSupabaseClient(
                "https://yforzdoqhocjorkshzfx.supabase.co",
                "",
                "filebridge-files"
        );
        assertFalse(emptyClient.isConfigured());

        DefaultSupabaseClient placeholderClient = new DefaultSupabaseClient(
                "https://yforzdoqhocjorkshzfx.supabase.co",
                "your-supabase-secret-key-here",
                "filebridge-files"
        );
        assertFalse(placeholderClient.isConfigured());
    }

    @Test
    @DisplayName("upload throws FileStorageException when unconfigured")
    void testUpload_UnconfiguredThrows() {
        DefaultSupabaseClient client = new DefaultSupabaseClient(
                "https://yforzdoqhocjorkshzfx.supabase.co",
                "",
                "filebridge-files"
        );
        assertThrows(FileStorageException.class, () ->
                client.upload("users/123/file.txt", "text/plain", new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("download throws FileStorageException when unconfigured")
    void testDownload_UnconfiguredThrows() {
        DefaultSupabaseClient client = new DefaultSupabaseClient(
                "https://yforzdoqhocjorkshzfx.supabase.co",
                "",
                "filebridge-files"
        );
        assertThrows(FileStorageException.class, () ->
                client.download("users/123/file.txt"));
    }

    @Test
    @DisplayName("path traversal in upload or download throws FileStorageException")
    void testPathTraversal_Throws() {
        DefaultSupabaseClient client = new DefaultSupabaseClient(
                "https://yforzdoqhocjorkshzfx.supabase.co",
                "valid-key",
                "filebridge-files"
        );
        assertThrows(FileStorageException.class, () ->
                client.upload("users/../../secret", "text/plain", new byte[]{1}));
        assertThrows(FileStorageException.class, () ->
                client.download("users/../../../etc/passwd"));
        assertThrows(FileStorageException.class, () ->
                client.exists("users/../../root"));
    }
}
