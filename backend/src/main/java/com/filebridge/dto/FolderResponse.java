package com.filebridge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public class FolderResponse {
    private String id;
    private String name;
    private String parentId;
    private String ownerId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;
    private boolean inTrash;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant deletedAt;
    private int fileCount;
    private int subfolderCount;

    public FolderResponse() {}

    private FolderResponse(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.parentId = builder.parentId;
        this.ownerId = builder.ownerId;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.inTrash = builder.inTrash;
        this.deletedAt = builder.deletedAt;
        this.fileCount = builder.fileCount;
        this.subfolderCount = builder.subfolderCount;
    }

    public static Builder builder() { return new Builder(); }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getParentId() { return parentId; }
    public String getOwnerId() { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public boolean isInTrash() { return inTrash; }
    public Instant getDeletedAt() { return deletedAt; }
    public int getFileCount() { return fileCount; }
    public int getSubfolderCount() { return subfolderCount; }

    public static class Builder {
        private String id;
        private String name;
        private String parentId;
        private String ownerId;
        private Instant createdAt;
        private Instant updatedAt;
        private boolean inTrash;
        private Instant deletedAt;
        private int fileCount;
        private int subfolderCount;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder parentId(String parentId) { this.parentId = parentId; return this; }
        public Builder ownerId(String ownerId) { this.ownerId = ownerId; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder inTrash(boolean inTrash) { this.inTrash = inTrash; return this; }
        public Builder deletedAt(Instant deletedAt) { this.deletedAt = deletedAt; return this; }
        public Builder fileCount(int fileCount) { this.fileCount = fileCount; return this; }
        public Builder subfolderCount(int subfolderCount) { this.subfolderCount = subfolderCount; return this; }

        public FolderResponse build() {
            return new FolderResponse(this);
        }
    }
}
