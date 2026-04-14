package com.aigrs.backend.controller;

import com.aigrs.backend.dto.response.ApiResponse;
import com.aigrs.backend.entity.FileEntity;
import com.aigrs.backend.exception.BadRequestException;
import com.aigrs.backend.exception.ResourceNotFoundException;
import com.aigrs.backend.repository.FileRepository;
import com.aigrs.backend.service.FileStorageService;
import com.aigrs.backend.util.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Files", description = "File upload, download, and management")
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileRepository fileRepository;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "video/mp4"
    );
    private static final long MAX_SIZE = 50 * 1024 * 1024; // 50MB
    private static final long IMAGE_COMPRESS_THRESHOLD = 2 * 1024 * 1024; // 2MB

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file (image/video, max 50MB)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "grievanceId", required = false) UUID grievanceId,
            Authentication auth) {

        UUID userId = UUID.fromString(auth.getName());
        UUID orgId = TenantContext.getOrgUUID();

        // Validate type
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("File type not allowed. Allowed: JPEG, PNG, MP4");
        }
        // Validate size
        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("File exceeds maximum size of 50MB");
        }

        String storedName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // Compress images > 2MB
        MultipartFile uploadFile = file;
        if (file.getContentType() != null && file.getContentType().startsWith("image/") && file.getSize() > IMAGE_COMPRESS_THRESHOLD) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Thumbnails.of(file.getInputStream())
                        .scale(1.0)
                        .outputQuality(0.6)
                        .toOutputStream(baos);
                log.info("Compressed image from {}KB to {}KB",
                        file.getSize() / 1024, baos.size() / 1024);
                // Upload compressed version
                String url = fileStorageService.upload(baos.toByteArray(), storedName, file.getContentType(), orgId);
                // Generate thumbnail
                ByteArrayOutputStream thumbBaos = new ByteArrayOutputStream();
                Thumbnails.of(file.getInputStream())
                        .size(200, 200)
                        .outputQuality(0.7)
                        .toOutputStream(thumbBaos);
                String thumbName = "thumb_" + storedName;
                String thumbnailUrl = fileStorageService.upload(thumbBaos.toByteArray(), thumbName, file.getContentType(), orgId);

                FileEntity fileEntity = saveFileEntity(file, storedName, url, thumbnailUrl, baos.size(), userId, grievanceId, orgId);
                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("File uploaded", buildFileResponse(fileEntity)));
            } catch (Exception e) {
                log.warn("Image compression failed, uploading original: {}", e.getMessage());
            }
        }

        // Upload original (no compression needed or compression failed)
        String url = fileStorageService.upload(file, storedName, orgId);
        String thumbnailUrl = null;

        // Generate thumbnail for images
        if (file.getContentType() != null && file.getContentType().startsWith("image/")) {
            try {
                ByteArrayOutputStream thumbBaos = new ByteArrayOutputStream();
                Thumbnails.of(file.getInputStream())
                        .size(200, 200)
                        .outputQuality(0.7)
                        .toOutputStream(thumbBaos);
                String thumbName = "thumb_" + storedName;
                thumbnailUrl = fileStorageService.upload(thumbBaos.toByteArray(), thumbName, file.getContentType(), orgId);
            } catch (Exception e) {
                log.warn("Thumbnail generation failed: {}", e.getMessage());
            }
        }

        FileEntity fileEntity = saveFileEntity(file, storedName, url, thumbnailUrl, file.getSize(), userId, grievanceId, orgId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("File uploaded", buildFileResponse(fileEntity)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get signed URL for a file (24h expiry)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFile(@PathVariable UUID id) {
        UUID orgId = TenantContext.getOrgUUID();
        FileEntity fileEntity = fileRepository.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        String signedUrl = fileStorageService.getSignedUrl(fileEntity.getStoredName(), orgId);

        Map<String, Object> response = new HashMap<>();
        response.put("fileId", fileEntity.getId());
        response.put("originalName", fileEntity.getOriginalName());
        response.put("url", signedUrl);
        response.put("contentType", fileEntity.getContentType());
        response.put("sizeBytes", fileEntity.getSizeBytes());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a file")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable UUID id) {
        UUID orgId = TenantContext.getOrgUUID();
        FileEntity fileEntity = fileRepository.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        fileEntity.setDeletedAt(LocalDateTime.now());
        fileRepository.save(fileEntity);

        return ResponseEntity.ok(ApiResponse.success("File deleted", null));
    }

    private FileEntity saveFileEntity(MultipartFile file, String storedName, String url, String thumbnailUrl,
                                       long size, UUID userId, UUID grievanceId, UUID orgId) {
        FileEntity entity = FileEntity.builder()
                .originalName(file.getOriginalFilename())
                .storedName(storedName)
                .url(url)
                .thumbnailUrl(thumbnailUrl)
                .contentType(file.getContentType())
                .sizeBytes(size)
                .uploadedBy(userId)
                .grievanceId(grievanceId)
                .build();
        entity.setOrgId(orgId);
        return fileRepository.save(entity);
    }

    private Map<String, Object> buildFileResponse(FileEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("fileId", entity.getId());
        map.put("url", entity.getUrl());
        map.put("thumbnailUrl", entity.getThumbnailUrl());
        map.put("originalName", entity.getOriginalName());
        return map;
    }
}
