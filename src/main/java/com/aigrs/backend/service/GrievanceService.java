package com.aigrs.backend.service;

import com.aigrs.backend.dto.request.*;
import com.aigrs.backend.dto.response.GrievanceResponse;
import com.aigrs.backend.entity.*;
import com.aigrs.backend.enums.GrievanceStatus;
import com.aigrs.backend.enums.Priority;
import com.aigrs.backend.exception.*;
import com.aigrs.backend.repository.*;
import com.aigrs.backend.util.GrievanceIdGenerator;
import com.aigrs.backend.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final CommentRepository commentRepository;
    private final RatingRepository ratingRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final GrievanceIdGenerator idGenerator;
    private final AiAnalysisService aiAnalysisService;
    private final SlaService slaService;
    private final NotificationService notificationService;

    /**
     * Full 11-step grievance submission workflow.
     */
    @Transactional
    public GrievanceResponse submitGrievance(GrievanceRequest request, UUID submitterId) {
        UUID orgId = TenantContext.getOrgUUID();

        // 1. Validate org exists
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        // 2. Build and save initial grievance
        Grievance grievance = Grievance.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .categoryId(request.getCategoryId())
                .departmentId(request.getDepartmentId())
                .submitterId(submitterId)
                .status(GrievanceStatus.SUBMITTED)
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .trackingId("TEMP") // Placeholder, will be replaced
                .build();
        grievance.setOrgId(orgId);

        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            grievance.setAttachmentIds(String.join(",",
                    request.getAttachmentIds().stream().map(UUID::toString).toList()));
        }

        grievance = grievanceRepository.save(grievance);

        // 3. Generate tracking ID
        String trackingId = idGenerator.generate(org.getCode(), orgId);
        grievance.setTrackingId(trackingId);

        // 4. Call AI analysis service
        AiAnalysisService.AiAnalysisResult aiResult =
                aiAnalysisService.analyze(request.getTitle(), request.getDescription(), orgId);

        // 5. Update grievance with AI results
        if (aiResult.getCategoryId() != null && grievance.getCategoryId() == null) {
            grievance.setCategoryId(aiResult.getCategoryId());
        }
        if (aiResult.getPriorityScore() != null) {
            grievance.setPriority(mapPriorityScore(aiResult.getPriorityScore()));
        }
        grievance.setIsDuplicate(aiResult.getIsDuplicate());
        grievance.setDuplicateOfId(aiResult.getDuplicateOfId());
        grievance.setAiSentiment(aiResult.getSentiment());
        grievance.setEstimatedHours(aiResult.getEstimatedHours());

        // 6. Auto-assign staff if suggested
        if (aiResult.getSuggestedStaffId() != null) {
            grievance.setAssignedStaffId(aiResult.getSuggestedStaffId());
            grievance.setStatus(GrievanceStatus.ASSIGNED);

            StaffAssignment assignment = StaffAssignment.builder()
                    .grievanceId(grievance.getId())
                    .staffId(aiResult.getSuggestedStaffId())
                    .assignedBy(submitterId)
                    .assignedAt(LocalDateTime.now())
                    .build();
            assignment.setOrgId(orgId);
            staffAssignmentRepository.save(assignment);
        }

        // 7. Calculate SLA deadline
        LocalDateTime slaDeadline = slaService.calculateAndStartSla(grievance);
        grievance.setSlaDeadline(slaDeadline);

        grievance = grievanceRepository.save(grievance);

        // 8-10. Create initial status history
        createStatusHistory(grievance.getId(), null, GrievanceStatus.SUBMITTED, submitterId, orgId, "Grievance submitted");

        // Trigger notifications (async, non-blocking)
        try {
            notificationService.onGrievanceSubmitted(grievance);
        } catch (Exception e) {
            log.warn("Failed to send submission notifications: {}", e.getMessage());
        }

        return toResponse(grievance);
    }

    public Page<GrievanceResponse> listGrievances(UUID submitterId, Pageable pageable) {
        UUID orgId = TenantContext.getOrgUUID();
        Page<Grievance> page;
        if (submitterId != null) {
            page = grievanceRepository.findBySubmitterIdAndOrgIdAndDeletedAtIsNull(submitterId, orgId, pageable);
        } else {
            page = grievanceRepository.findByOrgIdAndDeletedAtIsNull(orgId, pageable);
        }
        return page.map(this::toResponse);
    }

    public GrievanceResponse getGrievance(UUID id) {
        UUID orgId = TenantContext.getOrgUUID();
        Grievance grievance = grievanceRepository.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));
        return toResponse(grievance);
    }

    @Transactional
    public GrievanceResponse updateGrievance(UUID id, GrievanceRequest request, UUID userId) {
        UUID orgId = TenantContext.getOrgUUID();
        Grievance grievance = grievanceRepository.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        // Citizens can only edit within 1 hour of submission
        if (!grievance.getSubmitterId().equals(userId)) {
            throw new ForbiddenException("Only the submitter can edit this grievance");
        }
        if (grievance.getCreatedAt().plusHours(1).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Grievance can only be edited within 1 hour of submission");
        }

        grievance.setTitle(request.getTitle());
        grievance.setDescription(request.getDescription());
        grievance.setLocation(request.getLocation());
        grievance.setLatitude(request.getLatitude());
        grievance.setLongitude(request.getLongitude());
        if (request.getCategoryId() != null) grievance.setCategoryId(request.getCategoryId());
        if (request.getDepartmentId() != null) grievance.setDepartmentId(request.getDepartmentId());

        grievance = grievanceRepository.save(grievance);
        return toResponse(grievance);
    }

    @Transactional
    public void softDeleteGrievance(UUID id) {
        UUID orgId = TenantContext.getOrgUUID();
        Grievance grievance = grievanceRepository.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        grievance.setDeletedAt(LocalDateTime.now());
        grievanceRepository.save(grievance);
    }

    @Transactional
    public GrievanceResponse updateStatus(UUID id, StatusUpdateRequest request, UUID changedBy) {
        UUID orgId = TenantContext.getOrgUUID();
        Grievance grievance = grievanceRepository.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        GrievanceStatus oldStatus = grievance.getStatus();
        GrievanceStatus newStatus = request.getStatus();

        // Handle SLA pause/resume
        if (newStatus == GrievanceStatus.ON_HOLD) {
            slaService.pauseSla(grievance.getId(), orgId);
        } else if (oldStatus == GrievanceStatus.ON_HOLD) {
            LocalDateTime newDeadline = slaService.resumeSla(grievance);
            grievance.setSlaDeadline(newDeadline);
        }

        if (newStatus == GrievanceStatus.RESOLVED) {
            grievance.setResolvedAt(LocalDateTime.now());
        }

        grievance.setStatus(newStatus);
        grievance = grievanceRepository.save(grievance);

        createStatusHistory(id, oldStatus, newStatus, changedBy, orgId, request.getRemarks());

        // Notify citizen of status change
        try {
            notificationService.onStatusChanged(grievance, oldStatus, newStatus);
        } catch (Exception e) {
            log.warn("Failed to send status change notification: {}", e.getMessage());
        }

        return toResponse(grievance);
    }

    @Transactional
    public GrievanceResponse assignGrievance(UUID id, AssignRequest request, UUID assignedBy) {
        UUID orgId = TenantContext.getOrgUUID();
        Grievance grievance = grievanceRepository.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        // Deactivate previous assignment
        staffAssignmentRepository.findByGrievanceIdAndIsActiveTrue(id)
                .ifPresent(prev -> {
                    prev.setIsActive(false);
                    prev.setUnassignedAt(LocalDateTime.now());
                    staffAssignmentRepository.save(prev);
                });

        // Create new assignment
        StaffAssignment assignment = StaffAssignment.builder()
                .grievanceId(id)
                .staffId(request.getStaffId())
                .assignedBy(assignedBy)
                .assignedAt(LocalDateTime.now())
                .build();
        assignment.setOrgId(orgId);
        staffAssignmentRepository.save(assignment);

        grievance.setAssignedStaffId(request.getStaffId());
        if (grievance.getStatus() == GrievanceStatus.SUBMITTED) {
            grievance.setStatus(GrievanceStatus.ASSIGNED);
            createStatusHistory(id, GrievanceStatus.SUBMITTED, GrievanceStatus.ASSIGNED, assignedBy, orgId, "Assigned to staff");
        }
        grievance = grievanceRepository.save(grievance);

        try {
            notificationService.onGrievanceAssigned(grievance);
        } catch (Exception e) {
            log.warn("Failed to send assignment notification: {}", e.getMessage());
        }

        return toResponse(grievance);
    }

    @Transactional
    public GrievanceResponse transferGrievance(UUID id, UUID departmentId, UUID transferredBy) {
        UUID orgId = TenantContext.getOrgUUID();
        Grievance grievance = grievanceRepository.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        departmentRepository.findByIdAndOrgId(departmentId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        grievance.setDepartmentId(departmentId);
        // Unassign current staff since dept changed
        grievance.setAssignedStaffId(null);
        grievance = grievanceRepository.save(grievance);

        return toResponse(grievance);
    }

    @Transactional
    public GrievanceResponse escalateGrievance(UUID id, String reason, UUID escalatedBy) {
        UUID orgId = TenantContext.getOrgUUID();
        Grievance grievance = grievanceRepository.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        Escalation escalation = Escalation.builder()
                .grievanceId(id)
                .escalatedFrom(grievance.getAssignedStaffId())
                .reason(reason)
                .build();
        escalation.setOrgId(orgId);
        // Escalation repo save would go here — injecting EscalationRepository
        // For now, we'll just bump the priority
        if (grievance.getPriority() != Priority.CRITICAL) {
            Priority[] priorities = Priority.values();
            int currentIdx = grievance.getPriority().ordinal();
            if (currentIdx < priorities.length - 1) {
                grievance.setPriority(priorities[currentIdx + 1]);
            }
        }
        grievance = grievanceRepository.save(grievance);

        return toResponse(grievance);
    }

    @Transactional
    public GrievanceResponse reopenGrievance(UUID id, UUID userId) {
        UUID orgId = TenantContext.getOrgUUID();
        Grievance grievance = grievanceRepository.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        if (!grievance.getSubmitterId().equals(userId)) {
            throw new ForbiddenException("Only the submitter can reopen this grievance");
        }
        if (grievance.getStatus() != GrievanceStatus.RESOLVED) {
            throw new BadRequestException("Only resolved grievances can be reopened");
        }
        if (grievance.getResolvedAt() != null &&
                grievance.getResolvedAt().plusHours(48).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Grievance can only be reopened within 48 hours of resolution");
        }

        GrievanceStatus oldStatus = grievance.getStatus();
        grievance.setStatus(GrievanceStatus.REOPENED);
        grievance.setResolvedAt(null);
        grievance = grievanceRepository.save(grievance);

        createStatusHistory(id, oldStatus, GrievanceStatus.REOPENED, userId, orgId, "Reopened by citizen");

        return toResponse(grievance);
    }

    @Transactional
    public void rateGrievance(UUID grievanceId, RatingRequest request, UUID userId) {
        UUID orgId = TenantContext.getOrgUUID();
        Grievance grievance = grievanceRepository.findByIdAndOrgIdAndDeletedAtIsNull(grievanceId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        if (!grievance.getSubmitterId().equals(userId)) {
            throw new ForbiddenException("Only the submitter can rate this grievance");
        }
        if (grievance.getStatus() != GrievanceStatus.RESOLVED) {
            throw new BadRequestException("Can only rate resolved grievances");
        }
        if (ratingRepository.findByGrievanceIdAndUserId(grievanceId, userId).isPresent()) {
            throw new DuplicateResourceException("Grievance already rated");
        }

        Rating rating = Rating.builder()
                .grievanceId(grievanceId)
                .userId(userId)
                .score(request.getScore())
                .feedback(request.getFeedback())
                .build();
        rating.setOrgId(orgId);
        ratingRepository.save(rating);

        try {
            notificationService.onRatingReceived(grievance, request.getScore());
        } catch (Exception e) {
            log.warn("Failed to send rating notification: {}", e.getMessage());
        }
    }

    public Rating getRating(UUID grievanceId) {
        UUID orgId = TenantContext.getOrgUUID();
        return ratingRepository.findByGrievanceIdAndOrgId(grievanceId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating not found"));
    }

    // ---- Comments ----

    @Transactional
    public Comment addComment(UUID grievanceId, CommentRequest request, UUID userId) {
        UUID orgId = TenantContext.getOrgUUID();
        grievanceRepository.findByIdAndOrgIdAndDeletedAtIsNull(grievanceId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        Comment comment = Comment.builder()
                .grievanceId(grievanceId)
                .userId(userId)
                .content(request.getContent())
                .isInternal(request.getIsInternal() != null ? request.getIsInternal() : false)
                .build();
        comment.setOrgId(orgId);

        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            comment.setAttachmentIds(String.join(",",
                    request.getAttachmentIds().stream().map(UUID::toString).toList()));
        }

        comment = commentRepository.save(comment);

        try {
            notificationService.onCommentAdded(grievanceId, comment);
        } catch (Exception e) {
            log.warn("Failed to send comment notification: {}", e.getMessage());
        }

        return comment;
    }

    public Page<Comment> getComments(UUID grievanceId, boolean isStaff, Pageable pageable) {
        UUID orgId = TenantContext.getOrgUUID();
        if (isStaff) {
            return commentRepository.findByGrievanceIdAndOrgIdOrderByCreatedAtDesc(grievanceId, orgId, pageable);
        }
        return commentRepository.findByGrievanceIdAndOrgIdAndIsInternalFalseOrderByCreatedAtDesc(grievanceId, orgId, pageable);
    }

    public List<StatusHistory> getTimeline(UUID grievanceId) {
        UUID orgId = TenantContext.getOrgUUID();
        return statusHistoryRepository.findByGrievanceIdAndOrgIdOrderByCreatedAtAsc(grievanceId, orgId);
    }

    // ---- Helpers ----

    private void createStatusHistory(UUID grievanceId, GrievanceStatus from, GrievanceStatus to,
                                     UUID changedBy, UUID orgId, String remarks) {
        StatusHistory history = StatusHistory.builder()
                .grievanceId(grievanceId)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedBy)
                .remarks(remarks)
                .build();
        history.setOrgId(orgId);
        statusHistoryRepository.save(history);
    }

    private Priority mapPriorityScore(int score) {
        if (score >= 75) return Priority.CRITICAL;
        if (score >= 50) return Priority.HIGH;
        if (score >= 25) return Priority.MEDIUM;
        return Priority.LOW;
    }

    public GrievanceResponse toResponse(Grievance g) {
        GrievanceResponse.GrievanceResponseBuilder builder = GrievanceResponse.builder()
                .id(g.getId())
                .trackingId(g.getTrackingId())
                .title(g.getTitle())
                .description(g.getDescription())
                .status(g.getStatus())
                .priority(g.getPriority())
                .location(g.getLocation())
                .latitude(g.getLatitude())
                .longitude(g.getLongitude())
                .categoryId(g.getCategoryId())
                .departmentId(g.getDepartmentId())
                .submitterId(g.getSubmitterId())
                .assignedStaffId(g.getAssignedStaffId())
                .slaDeadline(g.getSlaDeadline())
                .resolvedAt(g.getResolvedAt())
                .isDuplicate(g.getIsDuplicate())
                .duplicateOfId(g.getDuplicateOfId())
                .aiSentiment(g.getAiSentiment())
                .estimatedHours(g.getEstimatedHours())
                .attachmentIds(g.getAttachmentIds())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt());

        // Eagerly loaded names if available
        if (g.getCategory() != null) builder.categoryName(g.getCategory().getName());
        if (g.getDepartment() != null) builder.departmentName(g.getDepartment().getName());
        if (g.getSubmitter() != null) builder.submitterName(g.getSubmitter().getName());
        if (g.getAssignedStaff() != null) builder.assignedStaffName(g.getAssignedStaff().getName());

        return builder.build();
    }
}
