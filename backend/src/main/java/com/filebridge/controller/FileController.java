package com.filebridge.controller;

import com.filebridge.dto.FileMetadataResponse;
import com.filebridge.dto.StorageUsageResponse;
import com.filebridge.model.FileMetadata;
import com.filebridge.security.UserPrincipal;
import com.filebridge.service.FileService;
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
     * POST /api/v1/files — upload a file owned by the authenticated user
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileMetadataResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        FileMetadataResponse response = fileService.uploadFile(file, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/files — list only current authenticated user's file metadata
     */
    @GetMapping
    public ResponseEntity<List<FileMetadataResponse>> listFiles(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.listFiles(principal.getId()));
    }

    /**
     * GET /api/v1/files/storage-usage — get storage usage stats for current user
     */
    @GetMapping("/storage-usage")
    public ResponseEntity<StorageUsageResponse> getStorageUsage(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.getStorageUsage(principal.getId()));
    }

    /**
     * GET /api/v1/files/{id} — get single file metadata only if owned
     */
    @GetMapping("/{id}")
    public ResponseEntity<FileMetadataResponse> getFileMetadata(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.getFileMetadata(id, principal.getId()));
    }

    /**
     * GET /api/v1/files/{id}/download — download file content only if owned
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        FileMetadata metadata = fileService.getFileEntity(id, principal.getId());
        InputStream inputStream = fileService.downloadFile(id, principal.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(metadata.getContentType()));
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
     * DELETE /api/v1/files/{id} — delete a file only if owned
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        fileService.deleteFile(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
