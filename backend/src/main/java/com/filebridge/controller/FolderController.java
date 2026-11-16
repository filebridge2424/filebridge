package com.filebridge.controller;

import com.filebridge.dto.FolderRequest;
import com.filebridge.dto.FolderResponse;
import com.filebridge.dto.RenameRequest;
import com.filebridge.security.UserPrincipal;
import com.filebridge.service.FolderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/folders")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    /**
     * POST /api/v1/folders - Create a folder
     */
    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(
            @Valid @RequestBody FolderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        FolderResponse response = folderService.createFolder(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/folders - List folders (optional parentId query param)
     */
    @GetMapping
    public ResponseEntity<List<FolderResponse>> listFolders(
            @RequestParam(value = "parentId", required = false) String parentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(folderService.listFolders(principal.getId(), parentId));
    }

    /**
     * GET /api/v1/folders/{id} - Get single folder
     */
    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getFolder(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(folderService.getFolder(id, principal.getId()));
    }

    /**
     * GET /api/v1/folders/{id}/lineage - Get breadcrumbs lineage
     */
    @GetMapping("/{id}/lineage")
    public ResponseEntity<List<FolderResponse>> getFolderLineage(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(folderService.getFolderLineage(id, principal.getId()));
    }

    /**
     * PATCH /api/v1/folders/{id}/rename - Rename folder
     */
    @PatchMapping("/{id}/rename")
    public ResponseEntity<FolderResponse> renameFolder(
            @PathVariable String id,
            @Valid @RequestBody RenameRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(folderService.renameFolder(id, request, principal.getId()));
    }

    /**
     * DELETE /api/v1/folders/{id} - Move folder to trash (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        folderService.moveFolderToTrash(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
