package com.aigrs.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileResponse {
    private UUID id;
    private String originalName;
    private String storedName;
    private String url;
    private String thumbnailUrl;
    private String contentType;
    private Long sizeBytes;
    private String uploadedByName;
    private UUID uploadedBy;
    private UUID grievanceId;
    private LocalDateTime createdAt;
}
