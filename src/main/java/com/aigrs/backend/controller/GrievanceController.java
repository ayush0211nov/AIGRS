package com.aigrs.backend.controller;

import com.aigrs.backend.dto.request.*;
import com.aigrs.backend.dto.response.ApiResponse;
import com.aigrs.backend.dto.response.GrievanceResponse;
import com.aigrs.backend.entity.Comment;
import com.aigrs.backend.entity.Rating;
import com.aigrs.backend.entity.StatusHistory;
import com.aigrs.backend.service.GrievanceService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/grievances")
@RequiredArgsConstructor
@Tag(name = "Grievances", description = "Grievance CRUD, comments, ratings, status management")
public class GrievanceController {

    private final GrievanceService grievanceService;

    @PostMapping
    @Operation(summary = "Submit a new grievance")
    public ResponseEntity<ApiResponse<GrievanceResponse>> submit(
            @Valid @RequestBody GrievanceRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        GrievanceResponse data = grievanceService.submitGrievance(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Grievance submitted successfully", data));
    }

    @GetMapping
    @Operation(summary = "List grievances (paginated, filterable)")
    public ResponseEntity<ApiResponse<Page<GrievanceResponse>>> list(
            @RequestParam(required = false) UUID submitterId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<GrievanceResponse> page = grievanceService.listGrievances(submitterId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single grievance with full details")
    public ResponseEntity<ApiResponse<GrievanceResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(grievanceService.getGrievance(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit grievance (citizen only, within 1 hour)")
    public ResponseEntity<ApiResponse<GrievanceResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody GrievanceRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Grievance updated", grievanceService.updateGrievance(id, request, userId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Soft delete grievance (admin only)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        grievanceService.softDeleteGrievance(id);
        return ResponseEntity.ok(ApiResponse.success("Grievance deleted", null));
    }

    // ---- Comments ----

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to a grievance")
    public ResponseEntity<ApiResponse<Comment>> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody CommentRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        Comment comment = grievanceService.addComment(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added", comment));
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "Get comments (internal filtered for non-staff)")
    public ResponseEntity<ApiResponse<Page<Comment>>> getComments(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication auth) {
        boolean isStaff = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") ||
                        a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_SUPERVISOR") ||
                        a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        return ResponseEntity.ok(ApiResponse.success(grievanceService.getComments(id, isStaff, pageable)));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Get status history timeline")
    public ResponseEntity<ApiResponse<List<StatusHistory>>> getTimeline(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(grievanceService.getTimeline(id)));
    }

    // ---- Status Management ----

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPERVISOR', 'SUPER_ADMIN')")
    @Operation(summary = "Update grievance status with remarks")
    public ResponseEntity<ApiResponse<GrievanceResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Status updated",
                grievanceService.updateStatus(id, request, userId)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'SUPER_ADMIN')")
    @Operation(summary = "Assign grievance to a staff member")
    public ResponseEntity<ApiResponse<GrievanceResponse>> assign(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Grievance assigned",
                grievanceService.assignGrievance(id, request, userId)));
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'SUPER_ADMIN')")
    @Operation(summary = "Transfer grievance to another department")
    public ResponseEntity<ApiResponse<GrievanceResponse>> transfer(
            @PathVariable UUID id,
            @RequestParam UUID departmentId,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Grievance transferred",
                grievanceService.transferGrievance(id, departmentId, userId)));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPERVISOR', 'SUPER_ADMIN')")
    @Operation(summary = "Manually escalate a grievance")
    public ResponseEntity<ApiResponse<GrievanceResponse>> escalate(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Grievance escalated",
                grievanceService.escalateGrievance(id, reason, userId)));
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reopen a resolved grievance (citizen, within 48h)")
    public ResponseEntity<ApiResponse<GrievanceResponse>> reopen(
            @PathVariable UUID id,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Grievance reopened",
                grievanceService.reopenGrievance(id, userId)));
    }

    @PostMapping("/{id}/rate")
    @Operation(summary = "Rate a resolved grievance (1-5)")
    public ResponseEntity<ApiResponse<Void>> rate(
            @PathVariable UUID id,
            @Valid @RequestBody RatingRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        grievanceService.rateGrievance(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rating submitted", null));
    }

    @GetMapping("/{id}/rate")
    @Operation(summary = "Get rating for a grievance")
    public ResponseEntity<ApiResponse<Rating>> getRating(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(grievanceService.getRating(id)));
    }
}
