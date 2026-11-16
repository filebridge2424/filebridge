package com.filebridge.dto;

import java.util.List;

public class TrashResponse {
    private List<FolderResponse> folders;
    private List<FileMetadataResponse> files;
    private long totalTrashBytes;

    public TrashResponse() {}

    public TrashResponse(List<FolderResponse> folders, List<FileMetadataResponse> files, long totalTrashBytes) {
        this.folders = folders;
        this.files = files;
        this.totalTrashBytes = totalTrashBytes;
    }

    public List<FolderResponse> getFolders() { return folders; }
    public void setFolders(List<FolderResponse> folders) { this.folders = folders; }

    public List<FileMetadataResponse> getFiles() { return files; }
    public void setFiles(List<FileMetadataResponse> files) { this.files = files; }

    public long getTotalTrashBytes() { return totalTrashBytes; }
    public void setTotalTrashBytes(long totalTrashBytes) { this.totalTrashBytes = totalTrashBytes; }
}
