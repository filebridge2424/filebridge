package com.filebridge.dto;

import com.filebridge.model.SharePermission;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class ShareRequest {

    @NotBlank(message = "File ID is required")
    private String fileId;

    private String recipientEmail;

    private SharePermission permission = SharePermission.VIEW;

    private boolean isLinkShare = false;

    private Instant expiresAt;

    public ShareRequest() {}

    public ShareRequest(String fileId, String recipientEmail, SharePermission permission, boolean isLinkShare, Instant expiresAt) {
        this.fileId = fileId;
        this.recipientEmail = recipientEmail;
        this.permission = permission != null ? permission : SharePermission.VIEW;
        this.isLinkShare = isLinkShare;
        this.expiresAt = expiresAt;
    }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public SharePermission getPermission() { return permission; }
    public void setPermission(SharePermission permission) { this.permission = permission; }

    public boolean isLinkShare() { return isLinkShare; }
    public void setLinkShare(boolean linkShare) { isLinkShare = linkShare; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
