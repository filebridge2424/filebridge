package com.filebridge.service;

import com.filebridge.dto.CreateOrderResponse;
import com.filebridge.dto.PaymentConfigResponse;
import com.filebridge.dto.PaymentResponse;
import com.filebridge.dto.VerifyPaymentRequest;
import com.filebridge.exception.AccessDeniedException;
import com.filebridge.exception.FileNotFoundException;
import com.filebridge.model.ActivityType;
import com.filebridge.model.PaymentTransaction;
import com.filebridge.model.User;
import com.filebridge.repository.PaymentTransactionRepository;
import com.filebridge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final String keyId;
    private final String keySecret;
    private final int premiumPriceInr;
    private final SubscriptionService subscriptionService;
    private final PaymentTransactionRepository paymentRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final HttpClient httpClient;

    public PaymentService(
            @Value("${filebridge.razorpay.key-id:rzp_test_placeholder_key}") String keyId,
            @Value("${filebridge.razorpay.key-secret:placeholder_secret}") String keySecret,
            @Value("${filebridge.subscription.premium-price-inr:199}") int premiumPriceInr,
            SubscriptionService subscriptionService,
            PaymentTransactionRepository paymentRepository,
            UserRepository userRepository,
            ActivityService activityService) {
        this.keyId = keyId != null ? keyId.trim() : "";
        this.keySecret = keySecret != null ? keySecret.trim() : "";
        this.premiumPriceInr = premiumPriceInr;
        this.subscriptionService = subscriptionService;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public PaymentConfigResponse getConfig() {
        return new PaymentConfigResponse(
                keyId,
                premiumPriceInr,
                premiumPriceInr * 100L,
                "INR",
                "PREMIUM"
        );
    }

    /**
     * Creates an order with Razorpay REST API or generates test order for local testing.
     */
    public CreateOrderResponse createOrder(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new FileNotFoundException("User not found with id: " + userId));

        long amountPaise = premiumPriceInr * 100L;
        String currency = "INR";
        String receipt = "rcpt_" + userId.substring(Math.max(0, userId.length() - 6)) + "_" + System.currentTimeMillis();

        String orderId = null;

        // Try calling official Razorpay REST API if valid key is set
        if (isLiveRazorpayConfigured()) {
            try {
                String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
                String jsonBody = String.format("{\"amount\":%d,\"currency\":\"%s\",\"receipt\":\"%s\",\"payment_capture\":1}",
                        amountPaise, currency, receipt);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.razorpay.com/v1/orders"))
                        .header("Authorization", "Basic " + auth)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    String body = response.body();
                    int idIndex = body.indexOf("\"id\":\"");
                    if (idIndex > 0) {
                        int endId = body.indexOf("\"", idIndex + 6);
                        orderId = body.substring(idIndex + 6, endId);
                        log.info("Created real Razorpay order: {}", orderId);
                    }
                } else {
                    log.warn("Razorpay API returned HTTP {}: {}", response.statusCode(), response.body());
                }
            } catch (Exception e) {
                log.warn("Error contacting Razorpay API: {}", e.getMessage());
            }
        }

        // Fallback for demonstration / test mode without real Razorpay credentials
        if (orderId == null) {
            orderId = "order_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            log.info("Generated demonstration order ID: {} for user: {}", orderId, user.getEmail());
        }

        return new CreateOrderResponse(orderId, amountPaise, currency, keyId, "PREMIUM");
    }

    /**
     * Verifies payment signature and upgrades user to PREMIUM.
     * Prevents duplicate payment submissions.
     */
    public PaymentResponse verifyPayment(VerifyPaymentRequest request, String userId) {
        if (request == null) {
            throw new IllegalArgumentException("Payment verification request cannot be null.");
        }

        String orderId = request.getRazorpayOrderId();
        String paymentId = request.getRazorpayPaymentId();
        String signature = request.getRazorpaySignature();

        if (orderId == null || paymentId == null || signature == null) {
            throw new IllegalArgumentException("Missing payment verification parameters.");
        }

        // Check for duplicate payment processing
        if (paymentRepository.existsByPaymentId(paymentId)) {
            log.warn("Duplicate payment attempt detected for payment ID: {}", paymentId);
            throw new IllegalArgumentException("This payment has already been verified and processed.");
        }

        // Verify cryptographic HMAC-SHA256 signature
        boolean signatureValid = verifySignature(orderId, paymentId, signature);
        if (!signatureValid) {
            log.error("Invalid payment signature for order: {} and payment: {}", orderId, paymentId);
            throw new AccessDeniedException("Payment signature verification failed. Untrusted payment.");
        }

        // Upgrade user in database
        User upgradedUser = subscriptionService.upgradeToPremium(userId, 30); // 30 days subscription

        // Record transaction in MongoDB
        PaymentTransaction transaction = new PaymentTransaction(
                userId,
                orderId,
                paymentId,
                signature,
                premiumPriceInr * 100L,
                "INR",
                "PREMIUM",
                "SUCCESS"
        );
        paymentRepository.save(transaction);

        activityService.logActivity(userId, ActivityType.PLAN_UPGRADED, null, null,
                "Upgraded to Premium plan (500 MB storage). Payment ID: " + paymentId);

        log.info("Successfully verified payment and upgraded user '{}' to PREMIUM", upgradedUser.getEmail());

        return new PaymentResponse(
                true,
                "Payment successfully verified! Your account has been upgraded to Premium (500 MB).",
                upgradedUser.getSubscriptionPlan(),
                upgradedUser.getSubscriptionStatus(),
                upgradedUser.getSubscriptionEndDate()
        );
    }

    /**
     * Verify HMAC SHA-256 signature.
     * signature = HMAC_SHA256(order_id + "|" + payment_id, secret)
     */
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        if (orderId == null || paymentId == null || signature == null) {
            return false;
        }

        // For local test mode demo with mock secret
        if ("placeholder_secret".equals(keySecret) || keySecret.isBlank()) {
            // In placeholder demo mode, check either test token signature or compute with placeholder secret
            if (signature.startsWith("test_sig_") || signature.equals("mock_valid_signature")) {
                return true;
            }
        }

        try {
            String data = orderId + "|" + paymentId;
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKeySpec);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = HexFormat.of().formatHex(hash);

            return MessageDigest.isEqual(
                    generatedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error computing signature verification: {}", e.getMessage());
            return false;
        }
    }

    private boolean isLiveRazorpayConfigured() {
        return !keyId.isBlank()
                && !keySecret.isBlank()
                && !keyId.contains("placeholder")
                && !keySecret.contains("placeholder");
    }

    public String getKeyId() { return keyId; }
}
