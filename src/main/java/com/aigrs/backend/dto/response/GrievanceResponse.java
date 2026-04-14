package com.aigrs.backend.dto.response;

import com.aigrs.backend.enums.GrievanceStatus;
import com.aigrs.backend.enums.Priority;
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
public class GrievanceResponse {
    private UUID id;
    private String trackingId;
    private String title;
    private String description;
    private GrievanceStatus status;
    private Priority priority;
    private String location;
    private Double latitude;
    private Double longitude;
    private String categoryName;
    private UUID categoryId;
    private String departmentName;
    private UUID departmentId;
    private String submitterName;
    private UUID submitterId;
    private String assignedStaffName;
    private UUID assignedStaffId;
    private LocalDateTime slaDeadline;
    private LocalDateTime resolvedAt;
    private Boolean isDuplicate;
    private UUID duplicateOfId;
    private String aiSentiment;
    private Double estimatedHours;
    private String attachmentIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
