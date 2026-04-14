package com.aigrs.backend.service;

import com.aigrs.backend.dto.response.GrievanceResponse;
import com.aigrs.backend.enums.GrievanceStatus;
import com.aigrs.backend.enums.Priority;
import com.aigrs.backend.repository.*;
import com.aigrs.backend.util.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final GrievanceRepository grievanceRepository;
    private final RatingRepository ratingRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Returns dashboard stats with 5-minute Redis cache.
     */
    public Map<String, Object> getStats(UUID orgId) {
        String cacheKey = "stats:" + orgId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, Map.class);
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed: {}", e.getMessage());
        }

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalGrievances", grievanceRepository.countByOrgIdAndDeletedAtIsNull(orgId));
        stats.put("pending", grievanceRepository.countByOrgIdAndStatusAndDeletedAtIsNull(orgId, GrievanceStatus.SUBMITTED));
        stats.put("inProgress", grievanceRepository.countByOrgIdAndStatusAndDeletedAtIsNull(orgId, GrievanceStatus.IN_PROGRESS));
        stats.put("resolved", grievanceRepository.countByOrgIdAndStatusAndDeletedAtIsNull(orgId, GrievanceStatus.RESOLVED));
        stats.put("rejected", grievanceRepository.countByOrgIdAndStatusAndDeletedAtIsNull(orgId, GrievanceStatus.REJECTED));
        stats.put("overdue", grievanceRepository.countOverdue(orgId, now));
        stats.put("avgResolutionHours", grievanceRepository.avgResolutionHours(orgId));
        stats.put("todaySubmitted", grievanceRepository.countByOrgIdAndDeletedAtIsNullAndCreatedAtAfter(orgId, todayStart));
        stats.put("todayResolved", grievanceRepository.countResolvedToday(orgId, todayStart));
        stats.put("citizenAvgRating", ratingRepository.avgRatingByOrg(orgId));

        // Calculate SLA compliance
        long total = (long) stats.get("totalGrievances");
        long overdue = (long) stats.get("overdue");
        double slaCompliance = total > 0 ? ((double)(total - overdue) / total) * 100 : 100.0;
        stats.put("slaCompliancePercentage", Math.round(slaCompliance * 100.0) / 100.0);

        // Cache for 5 minutes
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(stats), Duration.ofMinutes(5));
        } catch (Exception e) {
            log.warn("Redis cache write failed: {}", e.getMessage());
        }

        return stats;
    }

    public Map<String, Object> getCharts(UUID orgId) {
        Map<String, Object> charts = new HashMap<>();

        // Daily trend (last 30 days)
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        charts.put("dailyTrend", grievanceRepository.dailySubmittedTrend(orgId, from));

        // Priority breakdown
        List<Map<String, Object>> priorities = new ArrayList<>();
        for (Object[] row : grievanceRepository.priorityBreakdown(orgId)) {
            Map<String, Object> item = new HashMap<>();
            item.put("priority", row[0]);
            item.put("count", row[1]);
            priorities.add(item);
        }
        charts.put("priorityBreakdown", priorities);

        return charts;
    }

    public List<Map<String, Object>> getHeatmapData(UUID orgId) {
        List<Map<String, Object>> points = new ArrayList<>();
        for (Object[] row : grievanceRepository.heatmapData(orgId)) {
            Map<String, Object> point = new HashMap<>();
            point.put("lat", row[0]);
            point.put("lng", row[1]);
            point.put("count", row[2]);
            point.put("intensity", Math.min(1.0, ((Number)row[2]).doubleValue() / 10.0));
            points.add(point);
        }
        return points;
    }
}
