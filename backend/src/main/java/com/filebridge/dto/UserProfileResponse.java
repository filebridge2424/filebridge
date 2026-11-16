package com.filebridge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.filebridge.model.SubscriptionPlan;
import com.filebridge.model.SubscriptionStatus;

import java.time.Instant;

public class UserProfileResponse {

    private String id;
    private String name;
    private String email;
    private SubscriptionPlan subscriptionPlan;
    private SubscriptionStatus subscriptionStatus;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant subscriptionEndDate;

    public UserProfileResponse() {}

    public UserProfileResponse(String id, String name, String email) {
        this(id, name, email, SubscriptionPlan.FREE, SubscriptionStatus.ACTIVE, null);
    }

    public UserProfileResponse(String id, String name, String email,
                               SubscriptionPlan subscriptionPlan,
                               SubscriptionStatus subscriptionStatus,
                               Instant subscriptionEndDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.subscriptionPlan = subscriptionPlan != null ? subscriptionPlan : SubscriptionPlan.FREE;
        this.subscriptionStatus = subscriptionStatus != null ? subscriptionStatus : SubscriptionStatus.ACTIVE;
        this.subscriptionEndDate = subscriptionEndDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public SubscriptionPlan getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }

    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }

    public Instant getSubscriptionEndDate() { return subscriptionEndDate; }
    public void setSubscriptionEndDate(Instant subscriptionEndDate) { this.subscriptionEndDate = subscriptionEndDate; }
}
