package com.filebridge.controller;

import com.filebridge.dto.FileMetadataResponse;
import com.filebridge.dto.MoveRequest;
import com.filebridge.dto.RenameRequest;
import com.filebridge.dto.StorageUsageResponse;
import com.filebridge.model.FileMetadata;
import com.filebridge.security.UserPrincipal;
import com.filebridge.service.FileService;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * POST /api/v1/files - upload a file owned by the authenticated user with optional folderId
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileMetadataResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) String folderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        FileMetadataResponse response = fileService.uploadFile(file, folderId, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/files - list authenticated user's files with optional folderId, search, and sorting
     */
    @GetMapping
    public ResponseEntity<List<FileMetadataResponse>> listFiles(
            @RequestParam(value = "folderId", required = false) String folderId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortBy", required = false, defaultValue = "uploadedAt") String sortBy,
            @RequestParam(value = "sortDir", required = false, defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.listFiles(principal.getId(), folderId, search, sortBy, sortDir, false));
    }

    /**
     * GET /api/v1/files/recent - get recently uploaded files
     */
    @GetMapping("/recent")
    public ResponseEntity<List<FileMetadataResponse>> getRecentFiles(
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.getRecentFiles(principal.getId(), limit));
    }

    /**
     * GET /api/v1/files/storage-usage - get storage usage stats for current user
     */
    @GetMapping("/storage-usage")
    public ResponseEntity<StorageUsageResponse> getStorageUsage(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.getStorageUsage(principal.getId()));
    }

    /**
     * GET /api/v1/files/{id} - get single file metadata if owned or shared
     */
    @GetMapping("/{id}")
    public ResponseEntity<FileMetadataResponse> getFileMetadata(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.getFileMetadataAccessible(id, principal.getId(), principal.getEmail()));
    }

    /**
     * GET /api/v1/files/{id}/download - download file content with attachment disposition
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        FileMetadata metadata = fileService.getFileEntityAccessible(id, principal.getId(), principal.getEmail(), true);
        InputStream inputStream = fileService.downloadFileAccessible(id, principal.getId(), principal.getEmail(), false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                metadata.getContentType() != null ? metadata.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.setContentLength(metadata.getSizeBytes());
        headers.setContentDisposition(
            ContentDisposition.attachment()
                .filename(metadata.getOriginalFilename(), StandardCharsets.UTF_8)
                .build()
        );

        return ResponseEntity.ok()
            .headers(headers)
            .body(new InputStreamResource(inputStream));
    }

    /**
     * GET /api/v1/files/{id}/preview - inline preview stream for image, pdf, etc.
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<InputStreamResource> previewFile(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        FileMetadata metadata = fileService.getFileEntityAccessible(id, principal.getId(), principal.getEmail(), false);
        InputStream inputStream = fileService.downloadFileAccessible(id, principal.getId(), principal.getEmail(), true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                metadata.getContentType() != null ? metadata.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.setContentLength(metadata.getSizeBytes());
        headers.setContentDisposition(
            ContentDisposition.inline()
                .filename(metadata.getOriginalFilename(), StandardCharsets.UTF_8)
                .build()
        );

        return ResponseEntity.ok()
            .headers(headers)
            .body(new InputStreamResource(inputStream));
    }

    /**
     * PATCH /api/v1/files/{id}/rename - rename file
     */
    @PatchMapping("/{id}/rename")
    public ResponseEntity<FileMetadataResponse> renameFile(
            @PathVariable String id,
            @Valid @RequestBody RenameRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.renameFile(id, request.getNewName(), principal.getId()));
    }

    /**
     * PATCH /api/v1/files/{id}/move - move file between folders
     */
    @PatchMapping("/{id}/move")
    public ResponseEntity<FileMetadataResponse> moveFile(
            @PathVariable String id,
            @RequestBody MoveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String targetFolderId = request != null ? request.getTargetFolderId() : null;
        return ResponseEntity.ok(fileService.moveFile(id, targetFolderId, principal.getId()));
    }

    /**
     * DELETE /api/v1/files/{id} - soft delete (move to trash) or permanent delete if specified
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String id,
            @RequestParam(value = "permanent", required = false, defaultValue = "false") boolean permanent,
            @AuthenticationPrincipal UserPrincipal principal) {
        fileService.deleteFile(id, principal.getId(), permanent);
        return ResponseEntity.noContent().build();
    }
}
