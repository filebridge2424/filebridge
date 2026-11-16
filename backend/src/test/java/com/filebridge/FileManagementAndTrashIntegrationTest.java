package com.filebridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filebridge.dto.*;
import com.filebridge.model.FileMetadata;
import com.filebridge.model.Folder;
import com.filebridge.repository.FileMetadataRepository;
import com.filebridge.repository.FolderRepository;
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

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FileManagementAndTrashIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FileMetadataRepository fileRepository;

    @Autowired
    private FileStorageService storageService;

    @BeforeEach
    void setUp() {
        fileRepository.deleteAll();
        folderRepository.deleteAll();
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

    @Test
    @DisplayName("1. Folder creation, subfolder nesting, and duplicate name rejection")
    void testFolderCreationAndDuplicates() throws Exception {
        String token = registerAndGetToken("David", "david@example.com", "Password123!");

        // 1. Create root folder "Documents"
        FolderRequest rootReq = new FolderRequest("Documents", null);
        MvcResult rootRes = mockMvc.perform(post("/api/v1/folders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rootReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Documents"))
                .andExpect(jsonPath("$.parentId").doesNotExist())
                .andReturn();

        FolderResponse rootFolder = objectMapper.readValue(rootRes.getResponse().getContentAsString(), FolderResponse.class);
        assertNotNull(rootFolder.getId());

        // 2. Reject duplicate folder name under same parent
        mockMvc.perform(post("/api/v1/folders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rootReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("A folder named 'Documents' already exists in this location."));

        // 3. Create subfolder "Invoices" inside "Documents"
        FolderRequest subReq = new FolderRequest("Invoices", rootFolder.getId());
        mockMvc.perform(post("/api/v1/folders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Invoices"))
                .andExpect(jsonPath("$.parentId").value(rootFolder.getId()));
    }

    @Test
    @DisplayName("2. Folder renaming and lineage")
    void testFolderRenamingAndLineage() throws Exception {
        String token = registerAndGetToken("Emma", "emma@example.com", "Password123!");

        FolderRequest rootReq = new FolderRequest("Work", null);
        MvcResult rootRes = mockMvc.perform(post("/api/v1/folders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rootReq)))
                .andExpect(status().isCreated())
                .andReturn();
        FolderResponse root = objectMapper.readValue(rootRes.getResponse().getContentAsString(), FolderResponse.class);

        // Rename "Work" to "Work Projects"
        RenameRequest renameReq = new RenameRequest("Work Projects");
        mockMvc.perform(patch("/api/v1/folders/" + root.getId() + "/rename")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(renameReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Work Projects"));

        // Check lineage
        mockMvc.perform(get("/api/v1/folders/" + root.getId() + "/lineage")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Work Projects"));
    }

    @Test
    @DisplayName("3. File upload inside folder, renaming file, and moving file")
    void testFileUploadRenameAndMove() throws Exception {
        String token = registerAndGetToken("Frank", "frank@example.com", "Password123!");

        // Create folder A and folder B
        FolderRequest fAReq = new FolderRequest("Folder A", null);
        MvcResult fARes = mockMvc.perform(post("/api/v1/folders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fAReq)))
                .andExpect(status().isCreated())
                .andReturn();
        FolderResponse folderA = objectMapper.readValue(fARes.getResponse().getContentAsString(), FolderResponse.class);

        FolderRequest fBReq = new FolderRequest("Folder B", null);
        MvcResult fBRes = mockMvc.perform(post("/api/v1/folders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fBReq)))
                .andExpect(status().isCreated())
                .andReturn();
        FolderResponse folderB = objectMapper.readValue(fBRes.getResponse().getContentAsString(), FolderResponse.class);

        // Upload file into Folder A
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "Sample PDF content".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/files")
                .file(file)
                .param("folderId", folderA.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("report.pdf"))
                .andExpect(jsonPath("$.folderId").value(folderA.getId()))
                .andReturn();
        FileMetadataResponse uploaded = objectMapper.readValue(uploadRes.getResponse().getContentAsString(), FileMetadataResponse.class);

        // Rename file from report.pdf to annual-report (should retain .pdf)
        RenameRequest renameReq = new RenameRequest("annual-report");
        mockMvc.perform(patch("/api/v1/files/" + uploaded.getId() + "/rename")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(renameReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFilename").value("annual-report.pdf"));

        // Move file from Folder A to Folder B
        MoveRequest moveReq = new MoveRequest(folderB.getId());
        mockMvc.perform(patch("/api/v1/files/" + uploaded.getId() + "/move")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(moveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(folderB.getId()));

        // Verify Folder A now has 0 files, Folder B has 1 file
        mockMvc.perform(get("/api/v1/files?folderId=" + folderA.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/files?folderId=" + folderB.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].originalFilename").value("annual-report.pdf"));
    }

    @Test
    @DisplayName("4. Soft delete to Trash, restoration, and permanent deletion")
    void testTrashRestorationAndPermanentDelete() throws Exception {
        String token = registerAndGetToken("Grace", "grace_mgmt@example.com", "Password123!");

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "PNG bytes".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/files")
                .file(file)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        FileMetadataResponse uploaded = objectMapper.readValue(uploadRes.getResponse().getContentAsString(), FileMetadataResponse.class);

        // 1. Soft delete file
        mockMvc.perform(delete("/api/v1/files/" + uploaded.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // File should not be in active file list
        mockMvc.perform(get("/api/v1/files")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // File should appear in Trash
        mockMvc.perform(get("/api/v1/trash")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files.length()").value(1))
                .andExpect(jsonPath("$.files[0].originalFilename").value("photo.png"));

        // 2. Restore file
        mockMvc.perform(put("/api/v1/trash/files/" + uploaded.getId() + "/restore")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // File should be back in active file list
        mockMvc.perform(get("/api/v1/files")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 3. Move to trash again and permanently delete
        mockMvc.perform(delete("/api/v1/files/" + uploaded.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/trash/files/" + uploaded.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // File should be completely removed from MongoDB and disk storage
        assertFalse(fileRepository.existsById(uploaded.getId()));
    }

    @Test
    @DisplayName("5. File preview endpoint returns inline disposition and stream")
    void testFilePreviewEndpoint() throws Exception {
        String token = registerAndGetToken("Henry", "henry@example.com", "Password123!");

        byte[] imageBytes = "Fake PNG Image Content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "chart.png", "image/png", imageBytes
        );

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/files")
                .file(file)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        FileMetadataResponse uploaded = objectMapper.readValue(uploadRes.getResponse().getContentAsString(), FileMetadataResponse.class);

        // Preview endpoint
        mockMvc.perform(get("/api/v1/files/" + uploaded.getId() + "/preview")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline"))).andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("chart.png")))
                .andExpect(content().bytes(imageBytes));
    }

    @Test
    @DisplayName("6. IDOR prevention for folders: User A cannot access or delete User B's folder")
    void testFolderIdorPrevention() throws Exception {
        String tokenUserA = registerAndGetToken("User A", "user_a_folder@example.com", "Password123!");
        String tokenUserB = registerAndGetToken("User B", "user_b_folder@example.com", "Password123!");

        FolderRequest req = new FolderRequest("Private Folder", null);
        MvcResult res = mockMvc.perform(post("/api/v1/folders")
                .header("Authorization", "Bearer " + tokenUserB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        FolderResponse bFolder = objectMapper.readValue(res.getResponse().getContentAsString(), FolderResponse.class);

        // User A tries to GET User B's folder -> 404
        mockMvc.perform(get("/api/v1/folders/" + bFolder.getId())
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNotFound());

        // User A tries to RENAME User B's folder -> 404
        RenameRequest renameReq = new RenameRequest("Hacked Folder");
        mockMvc.perform(patch("/api/v1/folders/" + bFolder.getId() + "/rename")
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(renameReq)))
                .andExpect(status().isNotFound());

        // User A tries to DELETE User B's folder -> 404
        mockMvc.perform(delete("/api/v1/folders/" + bFolder.getId())
                .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNotFound());
    }
}
