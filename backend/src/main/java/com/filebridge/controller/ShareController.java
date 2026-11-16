package com.filebridge.controller;

import com.filebridge.dto.ShareRequest;
import com.filebridge.dto.ShareResponse;
import com.filebridge.dto.SharedFileResponse;
import com.filebridge.model.FileMetadata;
import com.filebridge.security.UserPrincipal;
import com.filebridge.service.FileShareService;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shares")
public class ShareController {

    private final FileShareService shareService;

    public ShareController(FileShareService shareService) {
        this.shareService = shareService;
    }

    /**
     * Share a file by email or generate a share link.
     */
    @PostMapping
    public ResponseEntity<ShareResponse> createShare(
            @Valid @RequestBody ShareRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ShareResponse response = shareService.shareFile(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all active shares for a file (owner only).
     */
    @GetMapping("/file/{fileId}")
    public ResponseEntity<List<ShareResponse>> getFileShares(
            @PathVariable String fileId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(shareService.getFileShares(fileId, principal.getId()));
    }

    /**
     * Revoke a share (owner only).
     */
    @DeleteMapping("/{shareId}")
    public ResponseEntity<Void> revokeShare(
            @PathVariable String shareId,
            @AuthenticationPrincipal UserPrincipal principal) {
        shareService.revokeShare(shareId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * List all files shared with the authenticated user.
     */
    @GetMapping("/shared-with-me")
    public ResponseEntity<List<SharedFileResponse>> getSharedWithMe(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(shareService.getSharedWithMe(principal.getId(), principal.getEmail()));
    }

    /**
     * Public endpoint: Get file metadata for a share link token without authentication.
     */
    @GetMapping("/public/{token}")
    public ResponseEntity<ShareResponse> getPublicShare(@PathVariable String token) {
        return ResponseEntity.ok(shareService.getPublicShareMetadata(token));
    }

    /**
     * Public endpoint: Download a shared file via link token (requires DOWNLOAD permission).
     */
    @GetMapping("/public/{token}/download")
    public ResponseEntity<InputStreamResource> downloadPublicShare(@PathVariable String token) {
        FileMetadata metadata = shareService.getPublicShareFile(token);
        InputStream stream = shareService.getPublicShareContent(token, true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                metadata.getContentType() != null ? metadata.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.setContentLength(metadata.getSizeBytes());
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(metadata.getOriginalFilename(), StandardCharsets.UTF_8)
                        .build()
        );

        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(stream));
    }

    /**
     * Public endpoint: Preview shared file via link token (allowed for VIEW and DOWNLOAD).
     */
    @GetMapping("/public/{token}/preview")
    public ResponseEntity<InputStreamResource> previewPublicShare(@PathVariable String token) {
        FileMetadata metadata = shareService.getPublicShareFile(token);
        InputStream stream = shareService.getPublicShareContent(token, false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                metadata.getContentType() != null ? metadata.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.setContentLength(metadata.getSizeBytes());
        headers.setContentDisposition(
                ContentDisposition.inline()
                        .filename(metadata.getOriginalFilename(), StandardCharsets.UTF_8)
                        .build()
        );

        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(stream));
    }

    /**
     * Download shared file for authenticated recipient (requires DOWNLOAD permission).
     */
    @GetMapping("/{shareId}/download")
    public ResponseEntity<InputStreamResource> downloadSharedFile(
            @PathVariable String shareId,
            @AuthenticationPrincipal UserPrincipal principal) {
        FileMetadata metadata = shareService.getSharedFile(shareId, principal.getId(), principal.getEmail());
        InputStream stream = shareService.getSharedFileContent(shareId, principal.getId(), principal.getEmail(), true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                metadata.getContentType() != null ? metadata.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.setContentLength(metadata.getSizeBytes());
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(metadata.getOriginalFilename(), StandardCharsets.UTF_8)
                        .build()
        );

        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(stream));
    }

    /**
     * Preview shared file for authenticated recipient.
     */
    @GetMapping("/{shareId}/preview")
    public ResponseEntity<InputStreamResource> previewSharedFile(
            @PathVariable String shareId,
            @AuthenticationPrincipal UserPrincipal principal) {
        FileMetadata metadata = shareService.getSharedFile(shareId, principal.getId(), principal.getEmail());
        InputStream stream = shareService.getSharedFileContent(shareId, principal.getId(), principal.getEmail(), false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                metadata.getContentType() != null ? metadata.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.setContentLength(metadata.getSizeBytes());
        headers.setContentDisposition(
                ContentDisposition.inline()
                        .filename(metadata.getOriginalFilename(), StandardCharsets.UTF_8)
                        .build()
        );

        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(stream));
    }
}
