package com.filebridge.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "file_shares")
public class FileShare {

    @Id
    private String id;

    @Indexed
    private String fileId;

    @Indexed
    private String ownerId;

    private String ownerEmail;

    private String ownerName;

    @Indexed
    private String sharedWithUserId;

    @Indexed
    private String sharedWithEmail;

    private SharePermission permission = SharePermission.VIEW;

    @Indexed(sparse = true)
    private String shareToken;

    private boolean isLinkShare;

    private Instant createdAt;

    private Instant expiresAt;

    private boolean revoked = false;

    public FileShare() {}

    private FileShare(Builder builder) {
        this.id = builder.id;
        this.fileId = builder.fileId;
        this.ownerId = builder.ownerId;
        this.ownerEmail = builder.ownerEmail;
        this.ownerName = builder.ownerName;
        this.sharedWithUserId = builder.sharedWithUserId;
        this.sharedWithEmail = builder.sharedWithEmail;
        this.permission = builder.permission != null ? builder.permission : SharePermission.VIEW;
        this.shareToken = builder.shareToken;
        this.isLinkShare = builder.isLinkShare;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.expiresAt = builder.expiresAt;
        this.revoked = builder.revoked;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getSharedWithUserId() { return sharedWithUserId; }
    public void setSharedWithUserId(String sharedWithUserId) { this.sharedWithUserId = sharedWithUserId; }

    public String getSharedWithEmail() { return sharedWithEmail; }
    public void setSharedWithEmail(String sharedWithEmail) { this.sharedWithEmail = sharedWithEmail; }

    public SharePermission getPermission() { return permission; }
    public void setPermission(SharePermission permission) { this.permission = permission; }

    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }

    public boolean isLinkShare() { return isLinkShare; }
    public void setLinkShare(boolean linkShare) { isLinkShare = linkShare; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }

    public static class Builder {
        private String id;
        private String fileId;
        private String ownerId;
        private String ownerEmail;
        private String ownerName;
        private String sharedWithUserId;
        private String sharedWithEmail;
        private SharePermission permission = SharePermission.VIEW;
        private String shareToken;
        private boolean isLinkShare = false;
        private Instant createdAt;
        private Instant expiresAt;
        private boolean revoked = false;

        public Builder id(String id) { this.id = id; return this; }
        public Builder fileId(String fileId) { this.fileId = fileId; return this; }
        public Builder ownerId(String ownerId) { this.ownerId = ownerId; return this; }
        public Builder ownerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; return this; }
        public Builder ownerName(String ownerName) { this.ownerName = ownerName; return this; }
        public Builder sharedWithUserId(String id) { this.sharedWithUserId = id; return this; }
        public Builder sharedWithEmail(String email) { this.sharedWithEmail = email; return this; }
        public Builder permission(SharePermission permission) { this.permission = permission; return this; }
        public Builder shareToken(String token) { this.shareToken = token; return this; }
        public Builder isLinkShare(boolean isLink) { this.isLinkShare = isLink; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder revoked(boolean revoked) { this.revoked = revoked; return this; }

        public FileShare build() {
            return new FileShare(this);
        }
    }
}
