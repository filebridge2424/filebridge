package com.filebridge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.filebridge.dto.ActivityResponse;
import com.filebridge.dto.AuthResponse;
import com.filebridge.dto.FileMetadataResponse;
import com.filebridge.dto.RegisterRequest;
import com.filebridge.dto.RenameRequest;
import com.filebridge.model.ActivityType;
import com.filebridge.repository.ActivityRepository;
import com.filebridge.repository.FileMetadataRepository;
import com.filebridge.repository.UserRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ActivityLoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetadataRepository fileRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @BeforeEach
    void setUp() {
        activityRepository.deleteAll();
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

    @Test
    @DisplayName("File lifecycle actions are logged and retrieved via activity timeline")
    void testFileLifecycleActivities() throws Exception {
        String token = registerAndGetToken("Henry", "henry@example.com", "password123");

        // 1. Upload
        MockMultipartFile file = new MockMultipartFile("file", "project_proposal.pdf", "application/pdf", "dummy pdf content".getBytes(StandardCharsets.UTF_8));
        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/files")
                .file(file)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        FileMetadataResponse uploaded = objectMapper.readValue(uploadRes.getResponse().getContentAsString(), FileMetadataResponse.class);

        // 2. Rename
        RenameRequest renameReq = new RenameRequest("final_proposal.pdf");
        mockMvc.perform(patch("/api/v1/files/" + uploaded.getId() + "/rename")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(renameReq)))
                .andExpect(status().isOk());

        // 3. Delete to trash
        mockMvc.perform(delete("/api/v1/files/" + uploaded.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // 4. Restore from trash
        mockMvc.perform(put("/api/v1/trash/files/" + uploaded.getId() + "/restore")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // 5. Query activity endpoint
        MvcResult actResult = mockMvc.perform(get("/api/v1/activity?page=0&size=10")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        List<ActivityResponse> activities = objectMapper.readValue(
                actResult.getResponse().getContentAsString(),
                new TypeReference<List<ActivityResponse>>() {});

        assertFalse(activities.isEmpty());
        assertTrue(activities.stream().anyMatch(a -> a.getEventType() == ActivityType.FILE_UPLOADED));
        assertTrue(activities.stream().anyMatch(a -> a.getEventType() == ActivityType.FILE_RENAMED));
        assertTrue(activities.stream().anyMatch(a -> a.getEventType() == ActivityType.FILE_DELETED));
        assertTrue(activities.stream().anyMatch(a -> a.getEventType() == ActivityType.FILE_RESTORED));
    }
}
