package com.filebridge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public class FileMetadataResponse {
    private String id;
    private String originalFilename;
    private String contentType;
    private long sizeBytes;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant uploadedAt;
    private String ownerId;

    private FileMetadataResponse() {}

    private FileMetadataResponse(Builder builder) {
        this.id = builder.id;
        this.originalFilename = builder.originalFilename;
        this.contentType = builder.contentType;
        this.sizeBytes = builder.sizeBytes;
        this.uploadedAt = builder.uploadedAt;
        this.ownerId = builder.ownerId;
    }

    public static Builder builder() { return new Builder(); }

    public String getId() { return id; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public Instant getUploadedAt() { return uploadedAt; }
    public String getOwnerId() { return ownerId; }

    public static class Builder {
        private String id;
        private String originalFilename;
        private String contentType;
        private long sizeBytes;
        private Instant uploadedAt;
        private String ownerId;

        public Builder id(String id) { this.id = id; return this; }
        public Builder originalFilename(String v) { this.originalFilename = v; return this; }
        public Builder contentType(String v) { this.contentType = v; return this; }
        public Builder sizeBytes(long v) { this.sizeBytes = v; return this; }
        public Builder uploadedAt(Instant v) { this.uploadedAt = v; return this; }
        public Builder ownerId(String v) { this.ownerId = v; return this; }

        public FileMetadataResponse build() { return new FileMetadataResponse(this); }
    }
}
