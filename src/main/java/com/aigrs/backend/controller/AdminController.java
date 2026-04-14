package com.aigrs.backend.controller;

import com.aigrs.backend.dto.response.ApiResponse;
import com.aigrs.backend.entity.Category;
import com.aigrs.backend.entity.Department;
import com.aigrs.backend.entity.User;
import com.aigrs.backend.enums.UserRole;
import com.aigrs.backend.exception.DuplicateResourceException;
import com.aigrs.backend.exception.ResourceNotFoundException;
import com.aigrs.backend.repository.*;
import com.aigrs.backend.service.DashboardService;
import com.aigrs.backend.service.GrievanceService;
import com.aigrs.backend.service.ReportExportService;
import com.aigrs.backend.util.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'SUPER_ADMIN')")
@Tag(name = "Admin", description = "Dashboard, staff management, categories, reports")
public class AdminController {

    private final DashboardService dashboardService;
    private final GrievanceService grievanceService;
    private final ReportExportService reportExportService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final RatingRepository ratingRepository;
    private final GrievanceRepository grievanceRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final OrganizationRepository organizationRepository;

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Real-time dashboard metrics (cached 5min)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        UUID orgId = TenantContext.getOrgUUID();
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStats(orgId)));
    }

    @GetMapping("/dashboard/charts")
    @Operation(summary = "Chart data: daily trends, priority breakdown")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCharts() {
        UUID orgId = TenantContext.getOrgUUID();
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getCharts(orgId)));
    }

    @GetMapping("/grievances")
    @Operation(summary = "All grievances (paginated, all filters)")
    public ResponseEntity<ApiResponse<Page<?>>> listAllGrievances(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(grievanceService.listGrievances(null, pageable)));
    }

    @GetMapping("/staff")
    @Operation(summary = "Staff list with performance metrics")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listStaff() {
        UUID orgId = TenantContext.getOrgUUID();
        List<User> staff = userRepository.findByOrgIdAndRoleIn(orgId, List.of(UserRole.STAFF, UserRole.SUPERVISOR));

        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : staff) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("name", u.getName());
            item.put("email", u.getEmail());
            item.put("phone", u.getPhone());
            item.put("role", u.getRole());
            item.put("assignedCount", staffAssignmentRepository.countByStaffIdAndIsActiveTrueAndOrgId(u.getId(), orgId));
            item.put("avgRating", ratingRepository.avgRatingByStaff(u.getId(), orgId));
            result.add(item);
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/staff/{id}/performance")
    @Operation(summary = "Individual staff performance metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> staffPerformance(@PathVariable UUID id) {
        UUID orgId = TenantContext.getOrgUUID();
        User staff = userRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));

        Map<String, Object> perf = new LinkedHashMap<>();
        perf.put("id", staff.getId());
        perf.put("name", staff.getName());
        perf.put("assignedCount", staffAssignmentRepository.countByStaffIdAndIsActiveTrueAndOrgId(id, orgId));
        perf.put("avgRating", ratingRepository.avgRatingByStaff(id, orgId));
        return ResponseEntity.ok(ApiResponse.success(perf));
    }

    @GetMapping("/departments")
    @Operation(summary = "List departments")
    public ResponseEntity<ApiResponse<List<Department>>> listDepartments() {
        UUID orgId = TenantContext.getOrgUUID();
        return ResponseEntity.ok(ApiResponse.success(departmentRepository.findByOrgIdAndIsActiveTrue(orgId)));
    }

    @GetMapping("/categories")
    @Operation(summary = "List categories")
    public ResponseEntity<ApiResponse<List<Category>>> listCategories() {
        UUID orgId = TenantContext.getOrgUUID();
        return ResponseEntity.ok(ApiResponse.success(categoryRepository.findByOrgIdAndIsActiveTrue(orgId)));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create category")
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestBody Category category) {
        UUID orgId = TenantContext.getOrgUUID();
        if (categoryRepository.existsByNameAndOrgId(category.getName(), orgId)) {
            throw new DuplicateResourceException("Category already exists");
        }
        category.setOrgId(orgId);
        return ResponseEntity.ok(ApiResponse.success("Category created", categoryRepository.save(category)));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update category")
    public ResponseEntity<ApiResponse<Category>> updateCategory(@PathVariable UUID id, @RequestBody Category update) {
        UUID orgId = TenantContext.getOrgUUID();
        Category category = categoryRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (update.getName() != null) category.setName(update.getName());
        if (update.getDescription() != null) category.setDescription(update.getDescription());
        if (update.getSlaHours() != null) category.setSlaHours(update.getSlaHours());
        if (update.getDepartmentId() != null) category.setDepartmentId(update.getDepartmentId());
        return ResponseEntity.ok(ApiResponse.success("Category updated", categoryRepository.save(category)));
    }

    @GetMapping("/reports/export")
    @Operation(summary = "Export grievances as CSV or Excel")
    public ResponseEntity<byte[]> exportReport(@RequestParam(defaultValue = "csv") String format) {
        UUID orgId = TenantContext.getOrgUUID();
        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            byte[] data = reportExportService.exportExcel(orgId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=grievances.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        }
        byte[] data = reportExportService.exportCsv(orgId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=grievances.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }

    @GetMapping("/analytics/heatmap")
    @Operation(summary = "Grievance location heatmap data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> heatmap() {
        UUID orgId = TenantContext.getOrgUUID();
        Map<String, Object> result = Map.of("points", dashboardService.getHeatmapData(orgId));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/sla/config")
    @Operation(summary = "Get SLA config for this organization")
    public ResponseEntity<ApiResponse<String>> getSlaConfig() {
        UUID orgId = TenantContext.getOrgUUID();
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        return ResponseEntity.ok(ApiResponse.success("SLA config retrieved", org.getSlaConfig()));
    }

    @PutMapping("/sla/config")
    @Operation(summary = "Update SLA config")
    public ResponseEntity<ApiResponse<Void>> updateSlaConfig(@RequestBody Map<String, Integer> config) {
        UUID orgId = TenantContext.getOrgUUID();
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        try {
            org.setSlaConfig(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(config));
            organizationRepository.save(org);
        } catch (Exception e) {
            throw new com.aigrs.backend.exception.BadRequestException("Invalid SLA config");
        }
        return ResponseEntity.ok(ApiResponse.success("SLA config updated", null));
    }
}
