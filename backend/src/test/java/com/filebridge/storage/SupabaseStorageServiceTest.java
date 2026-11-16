package com.filebridge.storage;

import com.filebridge.exception.FileNotFoundException;
import com.filebridge.exception.FileStorageException;
import com.filebridge.storage.supabase.SupabaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SupabaseStorageServiceTest {

    private SupabaseClient mockSupabaseClient;
    private SupabaseStorageService supabaseStorageService;

    @BeforeEach
    void setUp() {
        mockSupabaseClient = mock(SupabaseClient.class);
        supabaseStorageService = new SupabaseStorageService(mockSupabaseClient);
    }

    @Test
    @DisplayName("isConfigured delegates to SupabaseClient")
    void testIsConfigured_Delegation() {
        when(mockSupabaseClient.isConfigured()).thenReturn(true);
        assertTrue(supabaseStorageService.isConfigured());

        when(mockSupabaseClient.isConfigured()).thenReturn(false);
        assertFalse(supabaseStorageService.isConfigured());
    }

    @Test
    @DisplayName("store uploads bytes with Content-Type to SupabaseClient")
    void testStore_Success() {
        String key = "users/user123/document.pdf";
        byte[] fileBytes = "Test PDF Content".getBytes(StandardCharsets.UTF_8);

        supabaseStorageService.store(new ByteArrayInputStream(fileBytes), key, "application/pdf", fileBytes.length);

        verify(mockSupabaseClient).upload(eq(key), eq("application/pdf"), eq(fileBytes));
    }

    @Test
    @DisplayName("store propagates FileStorageException on upload error")
    void testStore_PropagatesError() {
        String key = "users/user123/document.pdf";
        byte[] fileBytes = "Test Content".getBytes(StandardCharsets.UTF_8);

        doThrow(new FileStorageException("Supabase upload failed"))
                .when(mockSupabaseClient).upload(anyString(), anyString(), any());

        assertThrows(FileStorageException.class, () ->
                supabaseStorageService.store(new ByteArrayInputStream(fileBytes), key, "application/pdf", fileBytes.length));
    }

    @Test
    @DisplayName("retrieve fetches stream from SupabaseClient")
    void testRetrieve_Success() throws Exception {
        String key = "users/user123/photo.jpg";
        byte[] expectedBytes = "IMAGE_BYTES".getBytes(StandardCharsets.UTF_8);
        when(mockSupabaseClient.download(key)).thenReturn(new ByteArrayInputStream(expectedBytes));

        InputStream stream = supabaseStorageService.retrieve(key);
        assertNotNull(stream);
        assertArrayEquals(expectedBytes, stream.readAllBytes());
        verify(mockSupabaseClient).download(key);
    }

    @Test
    @DisplayName("retrieve propagates FileNotFoundException when file missing")
    void testRetrieve_NotFound() {
        String key = "users/user123/missing.pdf";
        when(mockSupabaseClient.download(key)).thenThrow(new FileNotFoundException("File not found in Supabase"));

        assertThrows(FileNotFoundException.class, () -> supabaseStorageService.retrieve(key));
    }

    @Test
    @DisplayName("delete calls delete on SupabaseClient")
    void testDelete_Success() {
        String key = "users/user123/file.txt";
        supabaseStorageService.delete(key);
        verify(mockSupabaseClient).delete(List.of(key));
    }

    @Test
    @DisplayName("deleteMultiple calls delete with list of keys on SupabaseClient")
    void testDeleteMultiple_Success() {
        List<String> keys = List.of("users/u1/f1.txt", "users/u1/f2.txt");
        supabaseStorageService.deleteMultiple(keys);
        verify(mockSupabaseClient).delete(keys);
    }

    @Test
    @DisplayName("exists returns true when object exists")
    void testExists_ReturnsTrue() {
        String key = "users/user123/file.txt";
        when(mockSupabaseClient.exists(key)).thenReturn(true);
        assertTrue(supabaseStorageService.exists(key));
    }

    @Test
    @DisplayName("exists returns false when object does not exist")
    void testExists_ReturnsFalse() {
        String key = "users/user123/file.txt";
        when(mockSupabaseClient.exists(key)).thenReturn(false);
        assertFalse(supabaseStorageService.exists(key));
    }
}
