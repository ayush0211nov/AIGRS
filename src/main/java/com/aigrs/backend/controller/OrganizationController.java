package com.aigrs.backend.controller;

import com.aigrs.backend.dto.response.ApiResponse;
import com.aigrs.backend.entity.Organization;
import com.aigrs.backend.repository.OrganizationRepository;
import com.aigrs.backend.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Organizations", description = "Multi-tenant organization management (SUPER_ADMIN only)")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationRepository organizationRepository;

    @PostMapping
    @Operation(summary = "Register a new organization (creates admin user)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @RequestBody Map<String, Object> request) {
        Organization org = Organization.builder()
                .name((String) request.get("name"))
                .code((String) request.get("code"))
                .address((String) request.get("address"))
                .contactEmail((String) request.get("contactEmail"))
                .contactPhone((String) request.get("contactPhone"))
                .build();

        String adminEmail = (String) request.get("adminEmail");
        String adminPhone = (String) request.get("adminPhone");
        String adminPassword = (String) request.get("adminPassword");

        Map<String, Object> result = organizationService.createOrganization(org, adminEmail, adminPhone, adminPassword);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Organization created", result));
    }

    @GetMapping
    @Operation(summary = "List all organizations")
    public ResponseEntity<ApiResponse<List<Organization>>> list() {
        return ResponseEntity.ok(ApiResponse.success(organizationRepository.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization details")
    public ResponseEntity<ApiResponse<Organization>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getOrganization(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update organization settings")
    public ResponseEntity<ApiResponse<Organization>> update(@PathVariable UUID id, @RequestBody Organization update) {
        return ResponseEntity.ok(ApiResponse.success("Organization updated",
                organizationService.updateOrganization(id, update)));
    }

    @PutMapping("/{id}/suspend")
    @Operation(summary = "Suspend organization")
    public ResponseEntity<ApiResponse<Void>> suspend(@PathVariable UUID id) {
        organizationService.suspendOrganization(id);
        return ResponseEntity.ok(ApiResponse.success("Organization suspended", null));
    }

    @GetMapping("/{id}/usage")
    @Operation(summary = "Get usage stats (users, grievances, storage)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> usage(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getUsageStats(id)));
    }
}
