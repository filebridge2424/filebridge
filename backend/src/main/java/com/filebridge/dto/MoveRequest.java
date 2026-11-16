package com.filebridge.dto;

public class MoveRequest {

    /**
     * Target folder id. Null or empty string indicates moving to root directory.
     */
    private String targetFolderId;

    public MoveRequest() {}

    public MoveRequest(String targetFolderId) {
        this.targetFolderId = targetFolderId;
    }

    public String getTargetFolderId() { return targetFolderId; }
    public void setTargetFolderId(String targetFolderId) { this.targetFolderId = targetFolderId; }
}
