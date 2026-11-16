package com.filebridge.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "payment_transactions")
public class PaymentTransaction {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String orderId;

    @Indexed(unique = true)
    private String paymentId;

    private String signature;

    private long amountPaise;

    private String currency;

    private String plan;

    private String status; // "SUCCESS", "FAILED"

    private Instant createdAt;

    public PaymentTransaction() {}

    public PaymentTransaction(String userId, String orderId, String paymentId, String signature,
                              long amountPaise, String currency, String plan, String status) {
        this.userId = userId;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.signature = signature;
        this.amountPaise = amountPaise;
        this.currency = currency;
        this.plan = plan;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(long amountPaise) { this.amountPaise = amountPaise; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
