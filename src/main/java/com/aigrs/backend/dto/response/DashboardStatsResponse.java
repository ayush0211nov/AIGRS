package com.aigrs.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardStatsResponse {
    private Long totalGrievances;
    private Long openGrievances;
    private Long closedGrievances;
    private Long inProgressGrievances;
    private Long onHoldGrievances;
    private Long rejectedGrievances;
    
    private Double averageResolutionTime;
    private Long slaBreachCount;
    private Double slaCompliancePercentage;
    
    private Map<String, Long> statusDistribution;
    private Map<String, Long> priorityDistribution;
    private Map<String, Long> categoryDistribution;
    private List<DailyTrendData> dailyTrends;
    private List<DepartmentPerformance> departmentPerformance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrendData {
        private String date;
        private Long count;
        private Long resolved;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentPerformance {
        private String departmentName;
        private Long totalGrievances;
        private Long resolvedGrievances;
        private Double averageResolutionTime;
        private Long slaBreachCount;
    }
}
