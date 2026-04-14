package com.aigrs.backend.controller;

import com.aigrs.backend.dto.response.ApiResponse;
import com.aigrs.backend.dto.response.GrievanceResponse;
import com.aigrs.backend.repository.GrievanceRepository;
import com.aigrs.backend.service.GrievanceService;
import com.aigrs.backend.util.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Full-text search across grievances")
public class SearchController {

    private final GrievanceRepository grievanceRepository;
    private final GrievanceService grievanceService;

    @GetMapping
    @Operation(summary = "Full-text search grievances with filters")
    public ResponseEntity<ApiResponse<Page<GrievanceResponse>>> search(
            @RequestParam String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @PageableDefault(size = 20) Pageable pageable) {

        UUID orgId = TenantContext.getOrgUUID();

        // Use PostgreSQL full-text search
        Page<GrievanceResponse> results = grievanceRepository
                .fullTextSearch(orgId, q, pageable)
                .map(grievanceService::toResponse);

        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
