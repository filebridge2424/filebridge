package com.filebridge.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private String role; // e.g., "ROLE_USER"

    private Instant createdAt;

    // Subscription & Plan fields
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;

    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;

    private Instant subscriptionStartDate;

    private Instant subscriptionEndDate;

    private String razorpayCustomerId;

    public User() {}

    private User(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.email = builder.email;
        this.passwordHash = builder.passwordHash;
        this.role = builder.role;
        this.createdAt = builder.createdAt;
        this.subscriptionPlan = builder.subscriptionPlan != null ? builder.subscriptionPlan : SubscriptionPlan.FREE;
        this.subscriptionStatus = builder.subscriptionStatus != null ? builder.subscriptionStatus : SubscriptionStatus.ACTIVE;
        this.subscriptionStartDate = builder.subscriptionStartDate;
        this.subscriptionEndDate = builder.subscriptionEndDate;
        this.razorpayCustomerId = builder.razorpayCustomerId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan != null ? subscriptionPlan : SubscriptionPlan.FREE;
    }

    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus != null ? subscriptionStatus : SubscriptionStatus.ACTIVE;
    }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public Instant getSubscriptionStartDate() { return subscriptionStartDate; }
    public void setSubscriptionStartDate(Instant subscriptionStartDate) { this.subscriptionStartDate = subscriptionStartDate; }

    public Instant getSubscriptionEndDate() { return subscriptionEndDate; }
    public void setSubscriptionEndDate(Instant subscriptionEndDate) { this.subscriptionEndDate = subscriptionEndDate; }

    public String getRazorpayCustomerId() { return razorpayCustomerId; }
    public void setRazorpayCustomerId(String razorpayCustomerId) { this.razorpayCustomerId = razorpayCustomerId; }

    public boolean isPremium() {
        if (subscriptionPlan != SubscriptionPlan.PREMIUM) {
            return false;
        }
        if (subscriptionStatus != SubscriptionStatus.ACTIVE) {
            return false;
        }
        if (subscriptionEndDate != null && subscriptionEndDate.isBefore(Instant.now())) {
            return false;
        }
        return true;
    }

    public static class Builder {
        private String id;
        private String name;
        private String email;
        private String passwordHash;
        private String role;
        private Instant createdAt;
        private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;
        private SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;
        private Instant subscriptionStartDate;
        private Instant subscriptionEndDate;
        private String razorpayCustomerId;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder subscriptionPlan(SubscriptionPlan plan) { this.subscriptionPlan = plan; return this; }
        public Builder subscriptionStatus(SubscriptionStatus status) { this.subscriptionStatus = status; return this; }
        public Builder subscriptionStartDate(Instant start) { this.subscriptionStartDate = start; return this; }
        public Builder subscriptionEndDate(Instant end) { this.subscriptionEndDate = end; return this; }
        public Builder razorpayCustomerId(String id) { this.razorpayCustomerId = id; return this; }

        public User build() {
            return new User(this);
        }
    }
}
