package com.aigrs.backend.dto.response;

import com.aigrs.backend.enums.SlaEventType;
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
public class SlaEventResponse {
    private UUID id;
    private UUID grievanceId;
    private SlaEventType eventType;
    private LocalDateTime pausedAt;
    private LocalDateTime resumedAt;
    private Integer pausedDurationMinutes;
    private LocalDateTime createdAt;
}
