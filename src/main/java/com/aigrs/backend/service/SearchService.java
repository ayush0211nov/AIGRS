package com.aigrs.backend.service;

import com.aigrs.backend.dto.response.GrievanceResponse;
import com.aigrs.backend.dto.response.PaginatedResponse;
import com.aigrs.backend.entity.Grievance;
import com.aigrs.backend.enums.GrievanceStatus;
import com.aigrs.backend.enums.Priority;
import com.aigrs.backend.repository.GrievanceRepository;
import com.aigrs.backend.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final GrievanceRepository grievanceRepository;

    /**
     * Full-text search for grievances
     * Supports filtering and pagination
     */
    public PaginatedResponse<GrievanceResponse> searchGrievances(
            String query,
            String status,
            String priority,
            String categoryId,
            String departmentId,
            String submitterId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Integer page,
            Integer size,
            String sortBy
    ) {
        UUID orgId = TenantContext.getOrgUUID();
        
        // Build sort
        Sort sort = Sort.unsorted();
        if (sortBy != null) {
            switch (sortBy.toLowerCase()) {
                case "created":
                    sort = Sort.by(Sort.Direction.DESC, "createdAt");
                    break;
                case "updated":
                    sort = Sort.by(Sort.Direction.DESC, "updatedAt");
                    break;
                case "sla":
                    sort = Sort.by(Sort.Direction.ASC, "slaDeadline");
                    break;
                case "priority":
                    sort = Sort.by(Sort.Direction.DESC, "priority");
                    break;
            }
        }

        Pageable pageable = PageRequest.of(page, size, sort);

        // Apply filters using repository query with pagination
        var pageResult = grievanceRepository.findByOrgIdAndDeletedAtIsNull(orgId, Pageable.unpaged());
        List<Grievance> results = pageResult.getContent();

        // Apply text search filter
        if (query != null && !query.isEmpty()) {
            results = results.stream()
                    .filter(g -> g.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                            g.getDescription().toLowerCase().contains(query.toLowerCase()) ||
                            g.getTrackingId().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Apply status filter
        if (status != null && !status.isEmpty()) {
            try {
                GrievanceStatus statusEnum = GrievanceStatus.valueOf(status.toUpperCase());
                results = results.stream()
                        .filter(g -> g.getStatus().equals(statusEnum))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter: {}", status);
            }
        }

        // Apply priority filter
        if (priority != null && !priority.isEmpty()) {
            try {
                Priority priorityEnum = Priority.valueOf(priority.toUpperCase());
                results = results.stream()
                        .filter(g -> g.getPriority().equals(priorityEnum))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid priority filter: {}", priority);
            }
        }

        // Apply category filter
        if (categoryId != null && !categoryId.isEmpty()) {
            try {
                UUID catId = UUID.fromString(categoryId);
                results = results.stream()
                        .filter(g -> g.getCategoryId().equals(catId))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid category ID: {}", categoryId);
            }
        }

        // Apply department filter
        if (departmentId != null && !departmentId.isEmpty()) {
            try {
                UUID deptId = UUID.fromString(departmentId);
                results = results.stream()
                        .filter(g -> g.getDepartmentId().equals(deptId))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid department ID: {}", departmentId);
            }
        }

        // Apply submitter filter
        if (submitterId != null && !submitterId.isEmpty()) {
            try {
                UUID subId = UUID.fromString(submitterId);
                results = results.stream()
                        .filter(g -> g.getSubmitterId().equals(subId))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid submitter ID: {}", submitterId);
            }
        }

        // Apply date range filter
        if (fromDate != null) {
            results = results.stream()
                    .filter(g -> g.getCreatedAt().isAfter(fromDate) || g.getCreatedAt().isEqual(fromDate))
                    .collect(Collectors.toList());
        }

        if (toDate != null) {
            results = results.stream()
                    .filter(g -> g.getCreatedAt().isBefore(toDate) || g.getCreatedAt().isEqual(toDate))
                    .collect(Collectors.toList());
        }

        // Apply pagination
        long total = results.size();
        List<GrievanceResponse> content = results.stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PaginatedResponse.of(content, page, size, total);
    }

    /**
     * Search grievances by tracking ID
     */
    public GrievanceResponse searchByTrackingId(String trackingId) {
        UUID orgId = TenantContext.getOrgUUID();
        Grievance grievance = grievanceRepository.findByTrackingIdAndOrgId(trackingId, orgId)
                .orElse(null);
        return grievance != null ? mapToResponse(grievance) : null;
    }

    /**
     * Get grievances by submitter
     */
    public PaginatedResponse<GrievanceResponse> getPersonalGrievances(UUID submitterId, 
                                                                      Integer page, 
                                                                      Integer size) {
        UUID orgId = TenantContext.getOrgUUID();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        var pageResult = grievanceRepository.findBySubmitterIdAndOrgIdAndDeletedAtIsNull(
                submitterId, orgId, pageable);
        
        List<GrievanceResponse> content = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PaginatedResponse.of(content, page, size, pageResult.getTotalElements());
    }

    /**
     * Search for open grievances approaching SLA deadline
     */
    public List<GrievanceResponse> getGrievancesApproachingSlaDeadline(Long hoursThreshold) {
        UUID orgId = TenantContext.getOrgUUID();
        LocalDateTime threshold = LocalDateTime.now().plusHours(hoursThreshold);
        
        return grievanceRepository.findByOrgIdAndDeletedAtIsNull(orgId, Pageable.unpaged()).getContent().stream()
                .filter(g -> !g.getStatus().equals(GrievanceStatus.RESOLVED) &&
                        !g.getStatus().equals(GrievanceStatus.REJECTED))
                .filter(g -> g.getSlaDeadline() != null &&
                        g.getSlaDeadline().isBefore(threshold) &&
                        g.getSlaDeadline().isAfter(LocalDateTime.now()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get breached grievances
     */
    public List<GrievanceResponse> getBreachedGrievances() {
        UUID orgId = TenantContext.getOrgUUID();
        LocalDateTime now = LocalDateTime.now();
        
        return grievanceRepository.findByOrgIdAndDeletedAtIsNull(orgId, Pageable.unpaged()).getContent().stream()
                .filter(g -> !g.getStatus().equals(GrievanceStatus.RESOLVED) &&
                        !g.getStatus().equals(GrievanceStatus.REJECTED))
                .filter(g -> g.getSlaDeadline() != null && g.getSlaDeadline().isBefore(now))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private GrievanceResponse mapToResponse(Grievance grievance) {
        return GrievanceResponse.builder()
                .id(grievance.getId())
                .trackingId(grievance.getTrackingId())
                .title(grievance.getTitle())
                .description(grievance.getDescription())
                .status(grievance.getStatus())
                .priority(grievance.getPriority())
                .location(grievance.getLocation())
                .latitude(grievance.getLatitude())
                .longitude(grievance.getLongitude())
                .categoryId(grievance.getCategoryId())
                .departmentId(grievance.getDepartmentId())
                .submitterId(grievance.getSubmitterId())
                .assignedStaffId(grievance.getAssignedStaffId())
                .slaDeadline(grievance.getSlaDeadline())
                .resolvedAt(grievance.getResolvedAt())
                .isDuplicate(grievance.getIsDuplicate())
                .duplicateOfId(grievance.getDuplicateOfId())
                .aiSentiment(grievance.getAiSentiment())
                .estimatedHours(grievance.getEstimatedHours())
                .createdAt(grievance.getCreatedAt())
                .updatedAt(grievance.getUpdatedAt())
                .build();
    }
}
