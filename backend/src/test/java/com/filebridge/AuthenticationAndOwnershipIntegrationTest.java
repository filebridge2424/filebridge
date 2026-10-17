package com.filebridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filebridge.dto.AuthResponse;
import com.filebridge.dto.LoginRequest;
import com.filebridge.dto.RegisterRequest;
import com.filebridge.model.FileMetadata;
import com.filebridge.model.User;
import com.filebridge.repository.FileMetadataRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationAndOwnershipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetadataRepository fileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageService storageService;

    @BeforeEach
    void setUp() {
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
        assertNotNull(auth.getToken());
        return auth.getToken();
    }

    @Test
    @DisplayName("1. User registration succeeds and hashes password with BCrypt")
    void testSuccessfulRegistrationAndPasswordHashing() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "Password123!");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value("Alice"))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());

        User savedUser = userRepository.findByEmailIgnoreCase("alice@example.com").orElse(null);
        assertNotNull(savedUser);
        assertNotEquals("Password123!", savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches("Password123!", savedUser.getPasswordHash()));
        assertEquals("ROLE_USER", savedUser.getRole());
    }

    @Test
    @DisplayName("2. Duplicate email registration is rejected with 400 Bad Request")
    void testDuplicateEmailRegistration() throws Exception {
        registerAndGetToken("Bob First", "bob@example.com", "Password123!");

        RegisterRequest duplicateReq = new RegisterRequest("Bob Second", "bob@example.com", "DifferentPass123!");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("An account with this email address already exists."));
    }

    @Test
    @DisplayName("3. Successful login returns JWT token and safe user profile")
    void testSuccessfulLogin() throws Exception {
        registerAndGetToken("Charlie", "charlie@example.com", "CharliePass123!");

        LoginRequest loginReq = new LoginRequest("charlie@example.com", "CharliePass123!");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("charlie@example.com"));
    }

    @Test
    @DisplayName("4. Login with invalid credentials returns 401 Unauthorized")
    void testInvalidLoginCredentials() throws Exception {
        registerAndGetToken("Diana", "diana@example.com", "CorrectPassword123!");

        LoginRequest wrongPass = new LoginRequest("diana@example.com", "WrongPassword!");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPass)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password."));
    }

    @Test
    @DisplayName("5. Access to protected file endpoints without token returns 401 Unauthorized")
    void testProtectedEndpointsRequireToken() throws Exception {
        mockMvc.perform(get("/api/v1/files"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/files/storage-usage"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("6. Access with invalid or forged JWT returns 401 Unauthorized")
    void testInvalidOrForgedToken() throws Exception {
        mockMvc.perform(get("/api/v1/files")
                .header("Authorization", "Bearer invalid.token.signature"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("7. Uploaded file is owned by the authenticated user")
    void testUploadedFileOwnership() throws Exception {
        String token = registerAndGetToken("Edward", "edward@example.com", "Password123!");

        MockMultipartFile file = new MockMultipartFile(
                "file", "edward-doc.txt", "text/plain", "Hello Edward".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/files")
                .file(file)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("edward-doc.txt"))
                .andExpect(jsonPath("$.ownerId").isNotEmpty())
                .andReturn();

        User edward = userRepository.findByEmailIgnoreCase("edward@example.com").orElseThrow();
        FileMetadata savedMeta = fileRepository.findAll().get(0);
        assertEquals(edward.getId(), savedMeta.getOwnerId());
    }

    @Test
    @DisplayName("8. User A cannot list User B's files (Complete Isolation)")
    void testUserCannotListOtherUsersFiles() throws Exception {
        String tokenUserA = registerAndGetToken("User A", "usera@example.com", "Password123!");
        String tokenUserB = registerAndGetToken("User B", "userb@example.com", "Password123!");

        MockMultipartFile fileA = new MockMultipartFile(
                "file", "fileA.txt", "text/plain", "Content A".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/files")
                .file(fileA)
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isCreated());

        MockMultipartFile fileB = new MockMultipartFile(
                "file", "fileB.txt", "text/plain", "Content B".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/files")
                .file(fileB)
                .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isCreated());

        // User A lists files - must see ONLY fileA
        mockMvc.perform(get("/api/v1/files")
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].originalFilename").value("fileA.txt"));

        // User B lists files - must see ONLY fileB
        mockMvc.perform(get("/api/v1/files")
                .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].originalFilename").value("fileB.txt"));
    }

    @Test
    @DisplayName("9. User A cannot retrieve metadata, download, or delete User B's file (IDOR prevention)")
    void testIdorAttackPrevention() throws Exception {
        String tokenUserA = registerAndGetToken("Attacker A", "attackera@example.com", "Password123!");
        String tokenUserB = registerAndGetToken("Victim B", "victimb@example.com", "Password123!");

        MockMultipartFile victimFile = new MockMultipartFile(
                "file", "secret-plans.pdf", "application/pdf", "Super Secret".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/files")
                .file(victimFile)
                .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isCreated())
                .andReturn();

        FileMetadata victimFileMeta = fileRepository.findByOwnerId(
                userRepository.findByEmailIgnoreCase("victimb@example.com").orElseThrow().getId()
        ).get(0);
        String victimFileId = victimFileMeta.getId();

        // 1. Attacker A attempts GET /api/v1/files/{id} on victim's file
        mockMvc.perform(get("/api/v1/files/" + victimFileId)
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNotFound()); // Must return 404 without disclosing file exists

        // 2. Attacker A attempts GET /api/v1/files/{id}/download
        mockMvc.perform(get("/api/v1/files/" + victimFileId + "/download")
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNotFound());

        // 3. Attacker A attempts DELETE /api/v1/files/{id}
        mockMvc.perform(delete("/api/v1/files/" + victimFileId)
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNotFound());

        // Verify victim's file is intact in MongoDB and storage
        assertTrue(fileRepository.existsById(victimFileId));
        assertTrue(storageService.exists(victimFileMeta.getStorageKey()));

        // Verify Victim B can still download and delete their own file
        mockMvc.perform(get("/api/v1/files/" + victimFileId + "/download")
                .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/files/" + victimFileId)
                .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNoContent());

        assertFalse(fileRepository.existsById(victimFileId));
        assertFalse(storageService.exists(victimFileMeta.getStorageKey()));
    }

    @Test
    @DisplayName("10. Storage usage is strictly isolated per user")
    void testStorageUsagePerUser() throws Exception {
        String tokenUserA = registerAndGetToken("User A", "usage_a@example.com", "Password123!");
        String tokenUserB = registerAndGetToken("User B", "usage_b@example.com", "Password123!");

        // Upload 100 bytes for User A
        byte[] bytes100 = new byte[100];
        MockMultipartFile fileA = new MockMultipartFile("file", "fileA.bin", "application/octet-stream", bytes100);
        mockMvc.perform(multipart("/api/v1/files")
                .file(fileA)
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isCreated());

        // Upload 500 bytes for User B
        byte[] bytes500 = new byte[500];
        MockMultipartFile fileB = new MockMultipartFile("file", "fileB.bin", "application/octet-stream", bytes500);
        mockMvc.perform(multipart("/api/v1/files")
                .file(fileB)
                .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isCreated());

        // Verify User A usage is 100
        mockMvc.perform(get("/api/v1/files/storage-usage")
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedBytes").value(100));

        // Verify User B usage is 500
        mockMvc.perform(get("/api/v1/files/storage-usage")
                .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedBytes").value(500));
    }

    @Test
    @DisplayName("11. GET /api/v1/auth/me returns current user profile")
    void testGetCurrentUserProfile() throws Exception {
        String token = registerAndGetToken("Grace", "grace@example.com", "Password123!");

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Grace"))
                .andExpect(jsonPath("$.email").value("grace@example.com"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }
}
