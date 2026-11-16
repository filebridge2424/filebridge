package com.filebridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class FolderRequest {

    @NotBlank(message = "Folder name cannot be empty.")
    @Size(min = 1, max = 255, message = "Folder name must be between 1 and 255 characters.")
    @Pattern(regexp = "^[^\\\\/:*?\"<>|]+$", message = "Folder name contains invalid characters.")
    private String name;

    private String parentId;

    public FolderRequest() {}

    public FolderRequest(String name, String parentId) {
        this.name = name;
        this.parentId = parentId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
}
