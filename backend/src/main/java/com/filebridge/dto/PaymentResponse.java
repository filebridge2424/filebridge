package com.filebridge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.filebridge.model.SubscriptionPlan;
import com.filebridge.model.SubscriptionStatus;

import java.time.Instant;

public class PaymentResponse {

    private boolean success;
    private String message;
    private SubscriptionPlan plan;
    private SubscriptionStatus subscriptionStatus;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant subscriptionEndDate;

    public PaymentResponse() {}

    public PaymentResponse(boolean success, String message, SubscriptionPlan plan,
                           SubscriptionStatus subscriptionStatus, Instant subscriptionEndDate) {
        this.success = success;
        this.message = message;
        this.plan = plan;
        this.subscriptionStatus = subscriptionStatus;
        this.subscriptionEndDate = subscriptionEndDate;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public SubscriptionPlan getPlan() { return plan; }
    public void setPlan(SubscriptionPlan plan) { this.plan = plan; }

    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }

    public Instant getSubscriptionEndDate() { return subscriptionEndDate; }
    public void setSubscriptionEndDate(Instant subscriptionEndDate) { this.subscriptionEndDate = subscriptionEndDate; }
}
