package com.aigrs.backend.dto.response;

import com.aigrs.backend.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String body;
    private Map<String, Object> data;
    private Boolean isRead;
    private UUID grievanceId;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
