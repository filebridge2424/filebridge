package com.filebridge.dto;

public class StorageUsageResponse {
    private long usedBytes;
    private long limitBytes;
    private double usedPercent;

    private StorageUsageResponse() {}

    private StorageUsageResponse(Builder builder) {
        this.usedBytes = builder.usedBytes;
        this.limitBytes = builder.limitBytes;
        this.usedPercent = builder.usedPercent;
    }

    public static Builder builder() { return new Builder(); }

    public long getUsedBytes() { return usedBytes; }
    public long getLimitBytes() { return limitBytes; }
    public double getUsedPercent() { return usedPercent; }

    public static class Builder {
        private long usedBytes;
        private long limitBytes;
        private double usedPercent;

        public Builder usedBytes(long v) { this.usedBytes = v; return this; }
        public Builder limitBytes(long v) { this.limitBytes = v; return this; }
        public Builder usedPercent(double v) { this.usedPercent = v; return this; }

        public StorageUsageResponse build() { return new StorageUsageResponse(this); }
    }
}
