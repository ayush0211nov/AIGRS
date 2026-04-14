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
public class DepartmentResponse {
    private UUID id;
    private String name;
    private String description;
    private String headName;
    private UUID headUserId;
    private Boolean isActive;
    private Long grievanceCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
