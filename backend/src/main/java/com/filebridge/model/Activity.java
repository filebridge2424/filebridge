package com.filebridge.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "activities")
public class Activity {

    @Id
    private String id;

    @Indexed
    private String userId;

    private ActivityType eventType;

    private String fileId;

    private String fileName;

    private String description;

    @Indexed
    private Instant timestamp;

    public Activity() {}

    public Activity(String userId, ActivityType eventType, String fileId, String fileName, String description) {
        this.userId = userId;
        this.eventType = eventType;
        this.fileId = fileId;
        this.fileName = fileName;
        this.description = description;
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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
