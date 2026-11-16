package com.filebridge.storage;

import com.filebridge.storage.supabase.SupabaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DelegatingFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService localService;
    private SupabaseClient mockSupabaseClient;
    private SupabaseStorageService supabaseService;
    private DelegatingFileStorageService delegatingService;

    @BeforeEach
    void setUp() {
        localService = new LocalFileStorageService(tempDir.toString());
        mockSupabaseClient = mock(SupabaseClient.class);
        supabaseService = new SupabaseStorageService(mockSupabaseClient);
        delegatingService = new DelegatingFileStorageService(
                localService,
                supabaseService,
                "SUPABASE"
        );
    }

    @Test
    @DisplayName("Routes upload to Supabase when Supabase is active")
    void testStore_RoutesToSupabaseWhenActive() {
        when(mockSupabaseClient.isConfigured()).thenReturn(true);
        String key = "users/user1/doc.pdf";
        byte[] bytes = "Content".getBytes(StandardCharsets.UTF_8);

        delegatingService.store(new ByteArrayInputStream(bytes), key, "application/pdf", bytes.length);

        verify(mockSupabaseClient).upload(eq(key), eq("application/pdf"), eq(bytes));
        assertFalse(localService.exists(key));
    }

    @Test
    @DisplayName("Routes upload to Local when Supabase is not configured")
    void testStore_FallbackToLocalWhenUnconfigured() {
        when(mockSupabaseClient.isConfigured()).thenReturn(false);
        String key = "fallback-file.txt";
        byte[] bytes = "Local Content".getBytes(StandardCharsets.UTF_8);

        delegatingService.store(new ByteArrayInputStream(bytes), key, "text/plain", bytes.length);

        verify(mockSupabaseClient, never()).upload(any(), any(), any());
        assertTrue(localService.exists(key));
    }

    @Test
    @DisplayName("Routes retrieve to Supabase for cloud key starting with users/")
    void testRetrieve_RoutesToSupabaseForUsersPrefix() throws Exception {
        String key = "users/u123/image.png";
        byte[] expectedBytes = "CLOUD_IMAGE".getBytes(StandardCharsets.UTF_8);
        when(mockSupabaseClient.download(key)).thenReturn(new ByteArrayInputStream(expectedBytes));

        InputStream result = delegatingService.retrieve(key);
        assertNotNull(result);
        assertArrayEquals(expectedBytes, result.readAllBytes());
        verify(mockSupabaseClient).download(key);
    }

    @Test
    @DisplayName("Routes retrieve to Local for existing legacy local file")
    void testRetrieve_RoutesToLocalForExistingLegacyFile() throws Exception {
        String key = "legacy-file.txt";
        byte[] localData = "LEGACY_DATA".getBytes(StandardCharsets.UTF_8);
        localService.store(new ByteArrayInputStream(localData), key, "text/plain", localData.length);

        InputStream result = delegatingService.retrieve(key);
        assertNotNull(result);
        assertArrayEquals(localData, result.readAllBytes());
        verify(mockSupabaseClient, never()).download(any());
    }

    @Test
    @DisplayName("Routes delete to Supabase for cloud key and to local for legacy key")
    void testDelete_Routing() {
        String cloudKey = "users/u1/file.txt";
        String localKey = "local-del.txt";
        byte[] localData = "DATA".getBytes(StandardCharsets.UTF_8);
        localService.store(new ByteArrayInputStream(localData), localKey, "text/plain", localData.length);
        assertTrue(localService.exists(localKey));

        delegatingService.delete(cloudKey);
        verify(mockSupabaseClient).delete(java.util.List.of(cloudKey));

        delegatingService.delete(localKey);
        assertFalse(localService.exists(localKey));
    }
}
