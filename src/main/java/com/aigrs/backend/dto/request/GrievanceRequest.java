package com.aigrs.backend.dto.request;

import com.aigrs.backend.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Location is required")
    private String location;

    private Double latitude;
    private Double longitude;
    private UUID categoryId;
    private UUID departmentId;
    private Priority priority;
    private List<UUID> attachmentIds;
}
