package com.filebridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filebridge.dto.*;
import com.filebridge.model.SubscriptionPlan;
import com.filebridge.model.SubscriptionStatus;
import com.filebridge.repository.FileMetadataRepository;
import com.filebridge.repository.PaymentTransactionRepository;
import com.filebridge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SubscriptionAndPaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetadataRepository fileRepository;

    @Autowired
    private PaymentTransactionRepository paymentRepository;

    @Value("${filebridge.razorpay.key-secret:test_razorpay_secret}")
    private String keySecret;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
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

    private String computeHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    @Test
    @DisplayName("New registered user has FREE plan and default quota")
    void testNewUserHasFreePlan() throws Exception {
        String token = registerAndGetToken("David", "david@example.com", "password123");

        MvcResult result = mockMvc.perform(get("/api/v1/files/storage-usage")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        StorageUsageResponse usage = objectMapper.readValue(result.getResponse().getContentAsString(), StorageUsageResponse.class);
        assertEquals(SubscriptionPlan.FREE, usage.getPlan());
        assertEquals(SubscriptionStatus.ACTIVE, usage.getSubscriptionStatus());
        assertEquals(104857600L, usage.getLimitBytes()); // 100 MB
        assertEquals(0L, usage.getUsedBytes());
    }

    @Test
    @DisplayName("Create payment order and verify signature upgrades user to PREMIUM")
    void testPaymentOrderAndUpgrade() throws Exception {
        String token = registerAndGetToken("Eve", "eve@example.com", "password123");

        // Step 1: Create order
        MvcResult orderResult = mockMvc.perform(post("/api/v1/payments/create-order")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        CreateOrderResponse order = objectMapper.readValue(orderResult.getResponse().getContentAsString(), CreateOrderResponse.class);
        assertNotNull(order.getOrderId());
        assertEquals(19900L, order.getAmount());
        assertEquals("INR", order.getCurrency());

        // Step 2: Verify signature
        String paymentId = "pay_test_" + System.currentTimeMillis();
        String signature = computeHmacSha256(order.getOrderId() + "|" + paymentId, keySecret);

        VerifyPaymentRequest verifyReq = new VerifyPaymentRequest(order.getOrderId(), paymentId, signature);
        MvcResult verifyResult = mockMvc.perform(post("/api/v1/payments/verify")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andReturn();

        PaymentResponse paymentRes = objectMapper.readValue(verifyResult.getResponse().getContentAsString(), PaymentResponse.class);
        assertTrue(paymentRes.isSuccess());
        assertEquals(SubscriptionPlan.PREMIUM, paymentRes.getPlan());
        assertEquals(SubscriptionStatus.ACTIVE, paymentRes.getSubscriptionStatus());

        // Step 3: Check storage usage now reflects 500 MB limit
        MvcResult usageResult = mockMvc.perform(get("/api/v1/files/storage-usage")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        StorageUsageResponse usage = objectMapper.readValue(usageResult.getResponse().getContentAsString(), StorageUsageResponse.class);
        assertEquals(SubscriptionPlan.PREMIUM, usage.getPlan());
        assertEquals(524288000L, usage.getLimitBytes()); // 500 MB
    }

    @Test
    @DisplayName("Duplicate payment submissions are rejected")
    void testDuplicatePaymentRejected() throws Exception {
        String token = registerAndGetToken("Frank", "frank@example.com", "password123");

        MvcResult orderResult = mockMvc.perform(post("/api/v1/payments/create-order")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        CreateOrderResponse order = objectMapper.readValue(orderResult.getResponse().getContentAsString(), CreateOrderResponse.class);
        String paymentId = "pay_dup_" + System.currentTimeMillis();
        String signature = computeHmacSha256(order.getOrderId() + "|" + paymentId, keySecret);

        VerifyPaymentRequest verifyReq = new VerifyPaymentRequest(order.getOrderId(), paymentId, signature);

        // First verification succeeds
        mockMvc.perform(post("/api/v1/payments/verify")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk());

        // Duplicate verification rejected with 400 Bad Request
        mockMvc.perform(post("/api/v1/payments/verify")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Invalid payment signature is rejected with 403 Forbidden")
    void testInvalidSignatureRejected() throws Exception {
        String token = registerAndGetToken("Grace", "grace@example.com", "password123");

        VerifyPaymentRequest invalidReq = new VerifyPaymentRequest("order_fake_123", "pay_fake_456", "invalid_signature");
        mockMvc.perform(post("/api/v1/payments/verify")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isForbidden());
    }
}
