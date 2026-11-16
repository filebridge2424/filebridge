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

    private String storageType; // SUPABASE or LOCAL

    private String contentType;

    private long sizeBytes;

    private Instant uploadedAt;

    private Instant updatedAt;

    @Indexed
    private String ownerId;

    @Indexed
    private String folderId;

    @Indexed
    private boolean inTrash;

    private Instant deletedAt;

    public FileMetadata() {}

    private FileMetadata(Builder builder) {
        this.id = builder.id;
        this.originalFilename = builder.originalFilename;
        this.storageKey = builder.storageKey;
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

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getFolderId() { return folderId; }
    public void setFolderId(String folderId) { this.folderId = folderId; }

    public boolean isInTrash() { return inTrash; }
    public void setInTrash(boolean inTrash) { this.inTrash = inTrash; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public static class Builder {
        private String id;
        private String originalFilename;
        private String storageKey;
        private String storageType;
        private String contentType;
        private long sizeBytes;
        private Instant uploadedAt;
        private Instant updatedAt;
        private String ownerId;
        private String folderId;
        private boolean inTrash = false;
        private Instant deletedAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder originalFilename(String originalFilename) { this.originalFilename = originalFilename; return this; }
        public Builder storageKey(String storageKey) { this.storageKey = storageKey; return this; }
        public Builder storageType(String storageType) { this.storageType = storageType; return this; }
        public Builder contentType(String contentType) { this.contentType = contentType; return this; }
        public Builder sizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; return this; }
        public Builder uploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder ownerId(String ownerId) { this.ownerId = ownerId; return this; }
        public Builder folderId(String folderId) { this.folderId = folderId; return this; }
        public Builder inTrash(boolean inTrash) { this.inTrash = inTrash; return this; }
        public Builder deletedAt(Instant deletedAt) { this.deletedAt = deletedAt; return this; }

        public FileMetadata build() {
            return new FileMetadata(this);
        }
    }
}
