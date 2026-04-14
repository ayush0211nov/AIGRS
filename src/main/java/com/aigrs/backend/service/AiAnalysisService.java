package com.aigrs.backend.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Calls the external AI service for grievance analysis.
 * Falls back to defaults if the service is unavailable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {

    private final RestTemplate restTemplate;

    @Value("${ai-service.url}")
    private String aiServiceUrl;

    public AiAnalysisResult analyze(String title, String description, UUID orgId) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "title", title,
                    "description", description,
                    "orgId", orgId.toString()
            );

            String url = aiServiceUrl + "/api/v1/analyze";
            AiAnalysisResult result = restTemplate.postForObject(url, requestBody, AiAnalysisResult.class);

            if (result != null) {
                log.info("AI analysis completed for grievance: category={}, priority={}, duplicate={}",
                        result.getCategoryId(), result.getPriorityScore(), result.getIsDuplicate());
                return result;
            }
        } catch (Exception e) {
            log.warn("AI service unavailable, using defaults: {}", e.getMessage());
        }

        // Fallback defaults when AI service is unreachable
        return AiAnalysisResult.builder()
                .priorityScore(50)
                .isDuplicate(false)
                .sentiment("NEUTRAL")
                .estimatedHours(48.0)
                .build();
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AiAnalysisResult {
        private UUID categoryId;
        private Integer priorityScore;
        private Boolean isDuplicate;
        private UUID duplicateOfId;
        private UUID suggestedStaffId;
        private String sentiment;
        private Double estimatedHours;
    }
}
