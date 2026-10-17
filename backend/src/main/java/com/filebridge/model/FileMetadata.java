package com.filebridge.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "files")
public class FileMetadata {

    @Id
    private String id;

    @Indexed
    private String originalFilename;

    private String storageKey;

    private String contentType;

    private long sizeBytes;

    private Instant uploadedAt;

    @Indexed
    private String ownerId;

    public FileMetadata() {}

    private FileMetadata(Builder builder) {
        this.id = builder.id;
        this.originalFilename = builder.originalFilename;
        this.storageKey = builder.storageKey;
        this.contentType = builder.contentType;
        this.sizeBytes = builder.sizeBytes;
        this.uploadedAt = builder.uploadedAt;
        this.ownerId = builder.ownerId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public static class Builder {
        private String id;
        private String originalFilename;
        private String storageKey;
        private String contentType;
        private long sizeBytes;
        private Instant uploadedAt;
        private String ownerId;

        public Builder id(String id) { this.id = id; return this; }
        public Builder originalFilename(String originalFilename) { this.originalFilename = originalFilename; return this; }
        public Builder storageKey(String storageKey) { this.storageKey = storageKey; return this; }
        public Builder contentType(String contentType) { this.contentType = contentType; return this; }
        public Builder sizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; return this; }
        public Builder uploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; return this; }
        public Builder ownerId(String ownerId) { this.ownerId = ownerId; return this; }

        public FileMetadata build() {
            return new FileMetadata(this);
        }
    }
}
