package com.filebridge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public class FileMetadataResponse {
    private String id;
    private String originalFilename;
    private String storageType;
    private String contentType;
    private long sizeBytes;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant uploadedAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;
    private String ownerId;
    private String folderId;
    private boolean inTrash;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant deletedAt;

    private FileMetadataResponse() {}

    private FileMetadataResponse(Builder builder) {
        this.id = builder.id;
        this.originalFilename = builder.originalFilename;
        this.storageType = builder.storageType;
        this.contentType = builder.contentType;
        this.sizeBytes = builder.sizeBytes;
        this.uploadedAt = builder.uploadedAt;
        this.updatedAt = builder.updatedAt;
        this.ownerId = builder.ownerId;
        this.folderId = builder.folderId;
        this.inTrash = builder.inTrash;
        this.deletedAt = builder.deletedAt;
    }

    public static Builder builder() { return new Builder(); }

    public String getId() { return id; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStorageType() { return storageType; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public Instant getUploadedAt() { return uploadedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getOwnerId() { return ownerId; }
    public String getFolderId() { return folderId; }
    public boolean isInTrash() { return inTrash; }
    public Instant getDeletedAt() { return deletedAt; }

    public static class Builder {
        private String id;
        private String originalFilename;
        private String storageType;
        private String contentType;
        private long sizeBytes;
        private Instant uploadedAt;
        private Instant updatedAt;
        private String ownerId;
        private String folderId;
        private boolean inTrash;
        private Instant deletedAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder originalFilename(String v) { this.originalFilename = v; return this; }
        public Builder storageType(String v) { this.storageType = v; return this; }
        public Builder contentType(String v) { this.contentType = v; return this; }
        public Builder sizeBytes(long v) { this.sizeBytes = v; return this; }
        public Builder uploadedAt(Instant v) { this.uploadedAt = v; return this; }
        public Builder updatedAt(Instant v) { this.updatedAt = v; return this; }
        public Builder ownerId(String v) { this.ownerId = v; return this; }
        public Builder folderId(String v) { this.folderId = v; return this; }
        public Builder inTrash(boolean v) { this.inTrash = v; return this; }
        public Builder deletedAt(Instant v) { this.deletedAt = v; return this; }

        public FileMetadataResponse build() { return new FileMetadataResponse(this); }
    }
}
