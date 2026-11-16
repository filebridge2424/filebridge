package com.filebridge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.filebridge.dto.*;
import com.filebridge.model.FileMetadata;
import com.filebridge.model.SharePermission;
import com.filebridge.repository.FileMetadataRepository;
import com.filebridge.repository.FileShareRepository;
import com.filebridge.repository.UserRepository;
import com.filebridge.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FileSharingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetadataRepository fileRepository;

    @Autowired
    private FileShareRepository shareRepository;

    @Autowired
    private FileStorageService storageService;

    @BeforeEach
    void setUp() {
        shareRepository.deleteAll();
        fileRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndGetToken(String name, String email, String password) throws Exception {
        RegisterRequest registerReq = new RegisterRequest(name, email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        return auth.getToken();
    }

    private FileMetadataResponse uploadFile(String token, String filename, byte[] content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "text/plain", content);
        MvcResult result = mockMvc.perform(multipart("/api/v1/files")
                .file(file)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), FileMetadataResponse.class);
    }

    @Test
    @DisplayName("User can share file by email with VIEW permission; recipient cannot download")
    void testShareByEmailViewOnly() throws Exception {
        String aliceToken = registerAndGetToken("Alice", "alice@example.com", "password123");
        String bobToken = registerAndGetToken("Bob", "bob@example.com", "password123");

        FileMetadataResponse uploaded = uploadFile(aliceToken, "alice_notes.txt", "Important notes from Alice".getBytes(StandardCharsets.UTF_8));

        // Alice shares file with Bob (VIEW only)
        ShareRequest shareReq = new ShareRequest(uploaded.getId(), "bob@example.com", SharePermission.VIEW, false, null);
        MvcResult shareResult = mockMvc.perform(post("/api/v1/shares")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shareReq)))
                .andExpect(status().isCreated())
                .andReturn();

        ShareResponse shareRes = objectMapper.readValue(shareResult.getResponse().getContentAsString(), ShareResponse.class);
        assertNotNull(shareRes.getId());
        assertEquals(SharePermission.VIEW, shareRes.getPermission());
        assertEquals("bob@example.com", shareRes.getSharedWithEmail());

        // Bob checks "Shared with me"
        MvcResult sharedListResult = mockMvc.perform(get("/api/v1/shares/shared-with-me")
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andReturn();

        List<SharedFileResponse> sharedFiles = objectMapper.readValue(
                sharedListResult.getResponse().getContentAsString(),
                new TypeReference<List<SharedFileResponse>>() {});
        assertEquals(1, sharedFiles.size());
        assertEquals("alice_notes.txt", sharedFiles.get(0).getFilename());
        assertEquals(SharePermission.VIEW, sharedFiles.get(0).getPermission());

        // Bob can preview file
        mockMvc.perform(get("/api/v1/shares/" + shareRes.getId() + "/preview")
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        // Bob cannot download file (restricted because permission is VIEW)
        mockMvc.perform(get("/api/v1/shares/" + shareRes.getId() + "/download")
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User can share file by email with DOWNLOAD permission; recipient can download")
    void testShareByEmailDownloadPermission() throws Exception {
        String aliceToken = registerAndGetToken("Alice", "alice@example.com", "password123");
        String bobToken = registerAndGetToken("Bob", "bob@example.com", "password123");

        FileMetadataResponse uploaded = uploadFile(aliceToken, "dataset.csv", "id,name\n1,alpha".getBytes(StandardCharsets.UTF_8));

        // Alice shares with DOWNLOAD permission
        ShareRequest shareReq = new ShareRequest(uploaded.getId(), "bob@example.com", SharePermission.DOWNLOAD, false, null);
        MvcResult shareResult = mockMvc.perform(post("/api/v1/shares")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shareReq)))
                .andExpect(status().isCreated())
                .andReturn();

        ShareResponse shareRes = objectMapper.readValue(shareResult.getResponse().getContentAsString(), ShareResponse.class);

        // Bob can download file
        MvcResult downloadResult = mockMvc.perform(get("/api/v1/shares/" + shareRes.getId() + "/download")
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("id,name\n1,alpha", downloadResult.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Owner can revoke share; recipient access is revoked immediately")
    void testRevokeShareAccess() throws Exception {
        String aliceToken = registerAndGetToken("Alice", "alice@example.com", "password123");
        String bobToken = registerAndGetToken("Bob", "bob@example.com", "password123");

        FileMetadataResponse uploaded = uploadFile(aliceToken, "secret.txt", "Top secret".getBytes(StandardCharsets.UTF_8));

        ShareRequest shareReq = new ShareRequest(uploaded.getId(), "bob@example.com", SharePermission.DOWNLOAD, false, null);
        MvcResult shareResult = mockMvc.perform(post("/api/v1/shares")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shareReq)))
                .andExpect(status().isCreated())
                .andReturn();

        ShareResponse shareRes = objectMapper.readValue(shareResult.getResponse().getContentAsString(), ShareResponse.class);

        // Owner revokes share
        mockMvc.perform(delete("/api/v1/shares/" + shareRes.getId())
                .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNoContent());

        // Bob should no longer see it in "Shared with me"
        MvcResult sharedListResult = mockMvc.perform(get("/api/v1/shares/shared-with-me")
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andReturn();

        List<SharedFileResponse> sharedFiles = objectMapper.readValue(
                sharedListResult.getResponse().getContentAsString(),
                new TypeReference<List<SharedFileResponse>>() {});
        assertTrue(sharedFiles.isEmpty());

        // Bob's download attempt is rejected
        mockMvc.perform(get("/api/v1/shares/" + shareRes.getId() + "/download")
                .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Public share link allows unauthenticated access and respects permissions")
    void testPublicLinkShare() throws Exception {
        String aliceToken = registerAndGetToken("Alice", "alice@example.com", "password123");
        FileMetadataResponse uploaded = uploadFile(aliceToken, "public_doc.txt", "Public content".getBytes(StandardCharsets.UTF_8));

        // Generate public link with DOWNLOAD permission
        ShareRequest linkReq = new ShareRequest(uploaded.getId(), null, SharePermission.DOWNLOAD, true, null);
        MvcResult linkResult = mockMvc.perform(post("/api/v1/shares")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(linkReq)))
                .andExpect(status().isCreated())
                .andReturn();

        ShareResponse shareRes = objectMapper.readValue(linkResult.getResponse().getContentAsString(), ShareResponse.class);
        String token = shareRes.getShareToken();
        assertNotNull(token);

        // Public user without token can get metadata
        mockMvc.perform(get("/api/v1/shares/public/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("public_doc.txt"))
                .andExpect(jsonPath("$.ownerName").value("Alice"));

        // Public user can download
        MvcResult downloadResult = mockMvc.perform(get("/api/v1/shares/public/" + token + "/download"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("Public content", downloadResult.getResponse().getContentAsString());

        // Invalid token returns 403 Forbidden
        mockMvc.perform(get("/api/v1/shares/public/nonexistent-token"))
                .andExpect(status().isForbidden());
    }
}
