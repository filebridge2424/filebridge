package com.filebridge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.filebridge.model.SubscriptionPlan;
import com.filebridge.model.SubscriptionStatus;

import java.time.Instant;

public class StorageUsageResponse {
    private long usedBytes;
    private long limitBytes;
    private double usedPercent;
    private long remainingBytes;
    private long fileCount;
    private long trashBytes;
    private SubscriptionPlan plan;
    private SubscriptionStatus subscriptionStatus;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant subscriptionEndDate;

    private StorageUsageResponse() {}

    private StorageUsageResponse(Builder builder) {
        this.usedBytes = builder.usedBytes;
        this.limitBytes = builder.limitBytes;
        this.usedPercent = builder.usedPercent;
        this.remainingBytes = builder.remainingBytes;
        this.fileCount = builder.fileCount;
        this.trashBytes = builder.trashBytes;
        this.plan = builder.plan != null ? builder.plan : SubscriptionPlan.FREE;
        this.subscriptionStatus = builder.subscriptionStatus != null ? builder.subscriptionStatus : SubscriptionStatus.ACTIVE;
        this.subscriptionEndDate = builder.subscriptionEndDate;
    }

    public static Builder builder() { return new Builder(); }

    public long getUsedBytes() { return usedBytes; }
    public long getLimitBytes() { return limitBytes; }
    public double getUsedPercent() { return usedPercent; }
    public long getRemainingBytes() { return remainingBytes; }
    public long getFileCount() { return fileCount; }
    public long getTrashBytes() { return trashBytes; }
    public SubscriptionPlan getPlan() { return plan; }
    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public Instant getSubscriptionEndDate() { return subscriptionEndDate; }

    public static class Builder {
        private long usedBytes;
        private long limitBytes;
        private double usedPercent;
        private long remainingBytes;
        private long fileCount;
        private long trashBytes;
        private SubscriptionPlan plan = SubscriptionPlan.FREE;
        private SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;
        private Instant subscriptionEndDate;

        public Builder usedBytes(long v) { this.usedBytes = v; return this; }
        public Builder limitBytes(long v) { this.limitBytes = v; return this; }
        public Builder usedPercent(double v) { this.usedPercent = v; return this; }
        public Builder remainingBytes(long v) { this.remainingBytes = v; return this; }
        public Builder fileCount(long v) { this.fileCount = v; return this; }
        public Builder trashBytes(long v) { this.trashBytes = v; return this; }
        public Builder plan(SubscriptionPlan plan) { this.plan = plan; return this; }
        public Builder subscriptionStatus(SubscriptionStatus status) { this.subscriptionStatus = status; return this; }
        public Builder subscriptionEndDate(Instant end) { this.subscriptionEndDate = end; return this; }

        public StorageUsageResponse build() { return new StorageUsageResponse(this); }
    }
}
