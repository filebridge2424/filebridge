package com.filebridge.controller;

import com.filebridge.dto.CreateOrderResponse;
import com.filebridge.dto.PaymentConfigResponse;
import com.filebridge.dto.PaymentResponse;
import com.filebridge.dto.VerifyPaymentRequest;
import com.filebridge.security.UserPrincipal;
import com.filebridge.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Get Razorpay configuration (keyId, pricing, currency).
     */
    @GetMapping("/config")
    public ResponseEntity<PaymentConfigResponse> getConfig() {
        return ResponseEntity.ok(paymentService.getConfig());
    }

    /**
     * Create a Razorpay payment order for the current user.
     */
    @PostMapping("/create-order")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.createOrder(principal.getId()));
    }

    /**
     * Verify payment signature and upgrade user account to Premium.
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.verifyPayment(request, principal.getId()));
    }
}
