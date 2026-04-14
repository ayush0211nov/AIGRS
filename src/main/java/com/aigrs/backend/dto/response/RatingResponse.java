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
public class RatingResponse {
    private UUID id;
    private UUID grievanceId;
    private String userName;
    private UUID userId;
    private Integer score;
    private String feedback;
    private LocalDateTime createdAt;
}
