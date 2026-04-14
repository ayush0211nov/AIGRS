package com.aigrs.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Abstraction for file storage backends (S3, Cloudinary, local, etc.)
 */
public interface FileStorageService {

    /** Upload a file and return its public URL */
    String upload(MultipartFile file, String storedName, UUID orgId);

    /** Upload raw bytes (e.g., thumbnails) */
    String upload(byte[] data, String storedName, String contentType, UUID orgId);

    /** Generate a time-limited signed URL for private files */
    String getSignedUrl(String storedName, UUID orgId);

    /** Delete from storage backend */
    void delete(String storedName, UUID orgId);
}
