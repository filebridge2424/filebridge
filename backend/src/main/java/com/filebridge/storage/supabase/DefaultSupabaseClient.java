package com.filebridge.storage.supabase;

import com.filebridge.exception.FileNotFoundException;
import com.filebridge.exception.FileStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Standard implementation of SupabaseClient using Java's built-in HttpClient.
 */
@Component
public class DefaultSupabaseClient implements SupabaseClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultSupabaseClient.class);

    private final String supabaseUrl;
    private final String secretKey;
    private final String bucket;
    private final HttpClient httpClient;

    @Autowired
    public DefaultSupabaseClient(
            @Value("${filebridge.supabase.url:https://yforzdoqhocjorkshzfx.supabase.co}") String supabaseUrl,
            @Value("${filebridge.supabase.secret-key:}") String secretKey,
            @Value("${filebridge.supabase.bucket:filebridge-files}") String bucket) {
        this(supabaseUrl, secretKey, bucket, HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build());
    }

    public DefaultSupabaseClient(
            String supabaseUrl,
            String secretKey,
            String bucket,
            HttpClient httpClient) {
        this.supabaseUrl = trimTrailingSlash(supabaseUrl);
        this.secretKey = secretKey != null ? secretKey.trim() : "";
        this.bucket = bucket != null && !bucket.isBlank() ? bucket.trim() : "filebridge-files";
        this.httpClient = httpClient;

        if (isConfigured()) {
            log.info("DefaultSupabaseClient connected to URL: {} with bucket: {}", this.supabaseUrl, this.bucket);
        } else {
            log.warn("Supabase secret key is not set. Local storage will be used until SUPABASE_SECRET_KEY is provided.");
        }
    }

    @Override
    public boolean isConfigured() {
        return !secretKey.isBlank()
                && !secretKey.equalsIgnoreCase("placeholder")
                && !secretKey.contains("your-supabase-secret-key");
    }

    @Override
    public void upload(String path, String contentType, byte[] data) {
        assertConfigured();
        String encodedPath = encodePath(path);
        String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucket, encodedPath);

        String mime = (contentType != null && !contentType.isBlank()) ? contentType : "application/octet-stream";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Authorization", "Bearer " + secretKey)
                .header("apikey", secretKey)
                .header("Content-Type", mime)
                .header("x-upsert", "true")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.debug("Uploaded object to Supabase: {}", path);
            } else {
                throw new FileStorageException(String.format(
                        "Supabase upload failed for '%s' (HTTP %d): %s",
                        path, response.statusCode(), response.body()));
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new FileStorageException("Network error during Supabase upload: " + path, e);
        }
    }

    @Override
    public InputStream download(String path) {
        assertConfigured();
        String encodedPath = encodePath(path);
        String downloadUrl = String.format("%s/storage/v1/object/authenticated/%s/%s", supabaseUrl, bucket, encodedPath);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("Authorization", "Bearer " + secretKey)
                .header("apikey", secretKey)
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 404) {
                throw new FileNotFoundException("File not found in Supabase storage: " + path);
            }
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            throw new FileStorageException(String.format(
                    "Failed to retrieve file from Supabase: '%s' (HTTP %d)",
                    path, response.statusCode()));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new FileStorageException("Network error during Supabase download: " + path, e);
        }
    }

    @Override
    public void delete(List<String> paths) {
        if (!isConfigured() || paths == null || paths.isEmpty()) {
            return;
        }

        List<String> cleaned = paths.stream()
                .filter(p -> p != null && !p.isBlank())
                .map(this::normalizePath)
                .collect(Collectors.toList());

        if (cleaned.isEmpty()) return;

        String deleteUrl = String.format("%s/storage/v1/object/%s", supabaseUrl, bucket);
        StringBuilder jsonBody = new StringBuilder("{\"prefixes\":[");
        for (int i = 0; i < cleaned.size(); i++) {
            if (i > 0) jsonBody.append(",");
            jsonBody.append("\"").append(escapeJson(cleaned.get(i))).append("\"");
        }
        jsonBody.append("]}");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deleteUrl))
                .header("Authorization", "Bearer " + secretKey)
                .header("apikey", secretKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .method("DELETE", HttpRequest.BodyPublishers.ofString(jsonBody.toString(), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.debug("Deleted {} object(s) from Supabase: {}", cleaned.size(), cleaned);
            } else if (response.statusCode() == 404) {
                log.debug("Objects already absent from Supabase: {}", cleaned);
            } else {
                log.warn("Supabase delete returned HTTP {}: {}", response.statusCode(), response.body());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Error deleting objects from Supabase {}: {}", cleaned, e.getMessage());
        }
    }

    @Override
    public boolean exists(String path) {
        if (!isConfigured() || path == null || path.isBlank()) {
            return false;
        }

        String encodedPath = encodePath(path);
        String url = String.format("%s/storage/v1/object/authenticated/%s/%s", supabaseUrl, bucket, encodedPath);

        HttpRequest headReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + secretKey)
                .header("apikey", secretKey)
                .timeout(Duration.ofSeconds(10))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(headReq, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 200) return true;
            if (response.statusCode() == 404) return false;

            // Fallback for servers rejecting HEAD: GET with Range bytes=0-0
            HttpRequest rangeReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + secretKey)
                    .header("apikey", secretKey)
                    .header("Range", "bytes=0-0")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<Void> rangeRes = httpClient.send(rangeReq, HttpResponse.BodyHandlers.discarding());
            return rangeRes.statusCode() == 200 || rangeRes.statusCode() == 206;
        } catch (Exception e) {
            log.warn("Supabase exists check error for '{}': {}", path, e.getMessage());
            return false;
        }
    }

    private void assertConfigured() {
        if (!isConfigured()) {
            throw new FileStorageException(
                    "Supabase Storage credentials are not configured. "
                    + "Please define SUPABASE_SECRET_KEY in your environment.");
        }
    }

    private String encodePath(String path) {
        String normalized = normalizePath(path);
        String[] segments = normalized.split("/");
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) encoded.append("/");
            encoded.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return encoded.toString();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Object path must not be blank.");
        }
        if (path.contains("..")) {
            throw new FileStorageException("Invalid object path — path traversal detected: " + path);
        }
        String p = path.trim();
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String trimTrailingSlash(String url) {
        if (url == null) return "https://yforzdoqhocjorkshzfx.supabase.co";
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    @Override
    public String getSupabaseUrl() { return supabaseUrl; }

    @Override
    public String getBucket() { return bucket; }
}
