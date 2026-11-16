package com.filebridge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.filebridge.model.ActivityType;

import java.time.Instant;

public class ActivityResponse {

    private String id;
    private ActivityType eventType;
    private String fileId;
    private String fileName;
    private String description;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    public ActivityResponse() {}

    public ActivityResponse(String id, ActivityType eventType, String fileId, String fileName, String description, Instant timestamp) {
        this.id = id;
        this.eventType = eventType;
        this.fileId = fileId;
        this.fileName = fileName;
        this.description = description;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public ActivityType getEventType() { return eventType; }
    public void setEventType(ActivityType eventType) { this.eventType = eventType; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
