package com.filebridge.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "folders")
public class Folder {

    @Id
    private String id;

    @Indexed
    private String name;

    @Indexed
    private String parentId;

    @Indexed
    private String ownerId;

    private Instant createdAt;

    private Instant updatedAt;

    @Indexed
    private boolean inTrash;

    private Instant deletedAt;

    public Folder() {}

    private Folder(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.parentId = builder.parentId;
        this.ownerId = builder.ownerId;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.inTrash = builder.inTrash;
        this.deletedAt = builder.deletedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isInTrash() { return inTrash; }
    public void setInTrash(boolean inTrash) { this.inTrash = inTrash; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public static class Builder {
        private String id;
        private String name;
        private String parentId;
        private String ownerId;
        private Instant createdAt;
        private Instant updatedAt;
        private boolean inTrash = false;
        private Instant deletedAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder parentId(String parentId) { this.parentId = parentId; return this; }
        public Builder ownerId(String ownerId) { this.ownerId = ownerId; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder inTrash(boolean inTrash) { this.inTrash = inTrash; return this; }
        public Builder deletedAt(Instant deletedAt) { this.deletedAt = deletedAt; return this; }

        public Folder build() {
            return new Folder(this);
        }
    }
}
