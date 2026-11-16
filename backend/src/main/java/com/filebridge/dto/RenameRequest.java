package com.filebridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RenameRequest {

    @NotBlank(message = "New name cannot be empty.")
    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters.")
    @Pattern(regexp = "^[^\\\\/:*?\"<>|]+$", message = "Name contains invalid characters.")
    private String newName;

    public RenameRequest() {}

    public RenameRequest(String newName) {
        this.newName = newName;
    }

    public String getNewName() { return newName; }
    public void setNewName(String newName) { this.newName = newName; }
}
