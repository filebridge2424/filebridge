package com.filebridge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.filebridge.model.SharePermission;

import java.time.Instant;

public class SharedFileResponse {

    private String shareId;
    private String fileId;
    private String filename;
    private String contentType;
    private long sizeBytes;
    private String ownerName;
    private String ownerEmail;
    private SharePermission permission;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant sharedAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant expiresAt;

    public SharedFileResponse() {}

    public SharedFileResponse(String shareId, String fileId, String filename, String contentType,
                              long sizeBytes, String ownerName, String ownerEmail,
                              SharePermission permission, Instant sharedAt, Instant expiresAt) {
        this.shareId = shareId;
        this.fileId = fileId;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
        this.permission = permission;
        this.sharedAt = sharedAt;
        this.expiresAt = expiresAt;
    }

    public String getShareId() { return shareId; }
    public void setShareId(String shareId) { this.shareId = shareId; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public SharePermission getPermission() { return permission; }
    public void setPermission(SharePermission permission) { this.permission = permission; }

    public Instant getSharedAt() { return sharedAt; }
    public void setSharedAt(Instant sharedAt) { this.sharedAt = sharedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
