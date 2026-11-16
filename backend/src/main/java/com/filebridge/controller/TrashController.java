package com.filebridge.controller;

import com.filebridge.dto.FileMetadataResponse;
import com.filebridge.dto.FolderResponse;
import com.filebridge.dto.TrashResponse;
import com.filebridge.security.UserPrincipal;
import com.filebridge.service.FileService;
import com.filebridge.service.FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trash")
public class TrashController {

    private final FileService fileService;
    private final FolderService folderService;

    public TrashController(FileService fileService, FolderService folderService) {
        this.fileService = fileService;
        this.folderService = folderService;
    }

    /**
     * GET /api/v1/trash - List all trash items for current user
     */
    @GetMapping
    public ResponseEntity<TrashResponse> getTrash(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<FolderResponse> trashFolders = folderService.listTrashFolders(principal.getId());
        List<FileMetadataResponse> trashFiles = fileService.listFiles(principal.getId(), null, null, null, null, true);

        long totalBytes = trashFiles.stream().mapToLong(FileMetadataResponse::getSizeBytes).sum();
        return ResponseEntity.ok(new TrashResponse(trashFolders, trashFiles, totalBytes));
    }

    /**
     * PUT /api/v1/trash/files/{id}/restore - Restore a deleted file
     */
    @PutMapping("/files/{id}/restore")
    public ResponseEntity<Void> restoreFile(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        fileService.restoreFile(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/v1/trash/files/{id} - Permanently delete a file
     */
    @DeleteMapping("/files/{id}")
    public ResponseEntity<Void> deleteFilePermanently(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        fileService.deleteFilePermanently(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/trash/folders/{id}/restore - Restore a deleted folder
     */
    @PutMapping("/folders/{id}/restore")
    public ResponseEntity<Void> restoreFolder(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        folderService.restoreFolder(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/v1/trash/folders/{id} - Permanently delete a folder
     */
    @DeleteMapping("/folders/{id}")
    public ResponseEntity<Void> deleteFolderPermanently(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        folderService.permanentlyDeleteFolder(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/v1/trash/empty - Empty all trash for current user
     */
    @DeleteMapping("/empty")
    public ResponseEntity<Void> emptyTrash(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<FolderResponse> trashFolders = folderService.listTrashFolders(principal.getId());
        for (FolderResponse folder : trashFolders) {
            folderService.permanentlyDeleteFolder(folder.getId(), principal.getId());
        }

        List<FileMetadataResponse> trashFiles = fileService.listFiles(principal.getId(), null, null, null, null, true);
        for (FileMetadataResponse file : trashFiles) {
            fileService.deleteFilePermanently(file.getId(), principal.getId());
        }

        return ResponseEntity.noContent().build();
    }
}
