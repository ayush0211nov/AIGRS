package com.aigrs.backend.controller;

import com.aigrs.backend.dto.request.CommentRequest;
import com.aigrs.backend.dto.request.StatusUpdateRequest;
import com.aigrs.backend.dto.response.ApiResponse;
import com.aigrs.backend.dto.response.GrievanceResponse;
import com.aigrs.backend.entity.Comment;
import com.aigrs.backend.enums.GrievanceStatus;
import com.aigrs.backend.service.GrievanceService;
import com.aigrs.backend.util.TenantContext;
import com.aigrs.backend.repository.GrievanceRepository;
import com.aigrs.backend.repository.StaffAssignmentRepository;
import com.aigrs.backend.repository.RatingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'SUPERVISOR', 'ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Staff", description = "Staff-specific grievance management endpoints")
public class StaffController {

    private final GrievanceService grievanceService;
    private final GrievanceRepository grievanceRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final RatingRepository ratingRepository;

    @GetMapping("/my-grievances")
    @Operation(summary = "Get assigned grievances for current staff")
    public ResponseEntity<ApiResponse<Page<GrievanceResponse>>> myGrievances(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UUID orgId = TenantContext.getOrgUUID();
        Page<GrievanceResponse> page = grievanceRepository
                .findByAssignedStaffIdAndOrgIdAndDeletedAtIsNull(userId, orgId, pageable)
                .map(grievanceService::toResponse);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/my-grievances/stats")
    @Operation(summary = "Staff personal stats: assigned, resolved today, overdue, avg rating")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myStats(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UUID orgId = TenantContext.getOrgUUID();
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("assigned", staffAssignmentRepository.countByStaffIdAndIsActiveTrueAndOrgId(userId, orgId));
        stats.put("avgRating", ratingRepository.avgRatingByStaff(userId, orgId));
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PutMapping("/grievances/{id}/status")
    @Operation(summary = "Update grievance status with remarks")
    public ResponseEntity<ApiResponse<GrievanceResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Status updated",
                grievanceService.updateStatus(id, request, userId)));
    }

    @PostMapping("/grievances/{id}/proof")
    @Operation(summary = "Upload before/after photos as proof of resolution")
    public ResponseEntity<ApiResponse<Void>> uploadProof(
            @PathVariable UUID id,
            @RequestBody List<UUID> fileIds,
            Authentication auth) {
        // Add file IDs as attachments to the grievance
        UUID orgId = TenantContext.getOrgUUID();
        var grievance = grievanceRepository.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new com.aigrs.backend.exception.ResourceNotFoundException("Grievance not found"));

        String existing = grievance.getAttachmentIds() != null ? grievance.getAttachmentIds() + "," : "";
        grievance.setAttachmentIds(existing + String.join(",", fileIds.stream().map(UUID::toString).toList()));
        grievanceRepository.save(grievance);

        return ResponseEntity.ok(ApiResponse.success("Proof uploaded", null));
    }

    @PostMapping("/grievances/{id}/internal-comment")
    @Operation(summary = "Add internal note (not visible to citizen)")
    public ResponseEntity<ApiResponse<Comment>> internalComment(
            @PathVariable UUID id,
            @Valid @RequestBody CommentRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        request.setIsInternal(true);
        Comment comment = grievanceService.addComment(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Internal comment added", comment));
    }

    @PostMapping("/grievances/{id}/extension-request")
    @Operation(summary = "Request deadline extension with reason")
    public ResponseEntity<ApiResponse<Void>> extensionRequest(
            @PathVariable UUID id,
            @RequestParam String reason,
            @RequestParam(defaultValue = "24") int additionalHours,
            Authentication auth) {
        // Log extension request as internal comment and extend SLA
        UUID userId = UUID.fromString(auth.getName());
        CommentRequest comment = CommentRequest.builder()
                .content("Extension requested: " + reason + " (+" + additionalHours + "h)")
                .isInternal(true)
                .build();
        grievanceService.addComment(id, comment, userId);

        // Extend SLA deadline
        UUID orgId = TenantContext.getOrgUUID();
        var grievance = grievanceRepository.findByIdAndOrgIdAndDeletedAtIsNull(id, orgId)
                .orElseThrow(() -> new com.aigrs.backend.exception.ResourceNotFoundException("Grievance not found"));
        if (grievance.getSlaDeadline() != null) {
            grievance.setSlaDeadline(grievance.getSlaDeadline().plusHours(additionalHours));
            grievanceRepository.save(grievance);
        }

        return ResponseEntity.ok(ApiResponse.success("Extension request submitted", null));
    }
}
