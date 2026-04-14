package com.aigrs.backend.service;

import com.aigrs.backend.dto.response.UserResponse;
import com.aigrs.backend.entity.StaffAssignment;
import com.aigrs.backend.entity.User;
import com.aigrs.backend.enums.UserRole;
import com.aigrs.backend.exception.ForbiddenException;
import com.aigrs.backend.exception.ResourceNotFoundException;
import com.aigrs.backend.repository.StaffAssignmentRepository;
import com.aigrs.backend.repository.UserRepository;
import com.aigrs.backend.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffService {

    private final UserRepository userRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;

    /**
     * Get all active staff members for current organization
     */
    public List<UserResponse> getAllStaff() {
        UUID orgId = TenantContext.getOrgUUID();
        return userRepository.findByOrgIdAndIsActiveAndRoleIn(
                orgId,
                true,
                List.of(UserRole.STAFF, UserRole.MANAGER, UserRole.ADMIN)
        ).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get staff by department
     */
    public List<UserResponse> getStaffByDepartment(UUID departmentId) {
        UUID orgId = TenantContext.getOrgUUID();
        return userRepository.findByOrgIdAndDepartmentIdAndIsActiveAndRoleIn(
                orgId,
                departmentId,
                true,
                List.of(UserRole.STAFF, UserRole.MANAGER, UserRole.ADMIN)
        ).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get staff details
     */
    public UserResponse getStaffDetails(UUID staffId) {
        UUID orgId = TenantContext.getOrgUUID();
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

        if (!staff.getOrgId().equals(orgId)) {
            throw new ForbiddenException("Unauthorized access");
        }

        return mapToResponse(staff);
    }

    /**
     * Update staff notification preferences
     */
    @Transactional
    public UserResponse updateNotificationPreferences(UUID staffId, Boolean emailEnabled,
                                                       Boolean smsEnabled, Boolean pushEnabled) {
        UUID orgId = TenantContext.getOrgUUID();
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

        if (!staff.getOrgId().equals(orgId)) {
            throw new ForbiddenException("Unauthorized access");
        }

        if (emailEnabled != null) {
            staff.setEmailNotificationsEnabled(emailEnabled);
        }
        if (smsEnabled != null) {
            staff.setSmsNotificationsEnabled(smsEnabled);
        }
        if (pushEnabled != null) {
            staff.setPushNotificationsEnabled(pushEnabled);
        }

        User updated = userRepository.save(staff);
        return mapToResponse(updated);
    }

    /**
     * Get assigned grievances for staff
     */
    public Long getAssignedGrievanceCount(UUID staffId) {
        return staffAssignmentRepository.countByStaffIdAndIsActive(staffId, true);
    }

    /**
     * Get pending assignments
     */
    public List<StaffAssignment> getPendingAssignments(UUID staffId) {
        UUID orgId = TenantContext.getOrgUUID();
        return staffAssignmentRepository.findByStaffIdAndIsActiveAndOrgId(staffId, true, orgId);
    }

    /**
     * Validate if user is staff
     */
    public void validateStaffAccess(UUID userId) {
        UUID orgId = TenantContext.getOrgUUID();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getOrgId().equals(orgId)) {
            throw new ForbiddenException("Unauthorized access");
        }

        if (!List.of(UserRole.STAFF, UserRole.MANAGER, UserRole.ADMIN).contains(user.getRole())) {
            throw new ForbiddenException("User is not a staff member");
        }
    }

    /**
     * Get staff workload statistics
     */
    public StaffWorkloadStats getStaffWorkload(UUID staffId) {
        UUID orgId = TenantContext.getOrgUUID();
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

        if (!staff.getOrgId().equals(orgId)) {
            throw new ForbiddenException("Unauthorized access");
        }

        Long activeAssignments = staffAssignmentRepository.countByStaffIdAndIsActive(staffId, true);
        List<StaffAssignment> assignments = staffAssignmentRepository
                .findByStaffIdAndIsActiveAndOrgId(staffId, true, orgId);

        return StaffWorkloadStats.builder()
                .staffId(staffId)
                .name(staff.getName())
                .activeAssignmentCount(activeAssignments)
                .isOverloaded(activeAssignments > 10) // Threshold: 10 active grievances
                .lastAssignmentTime(assignments.isEmpty() ? null : 
                        assignments.stream()
                                .map(StaffAssignment::getAssignedAt)
                                .max(LocalDateTime::compareTo)
                                .orElse(null))
                .build();
    }

    private UserResponse mapToResponse(User user) {
        String departmentName = user.getDepartment() != null ? user.getDepartment().getName() : null;
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .orgId(user.getOrgId())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .departmentName(departmentName)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .emailNotificationsEnabled(user.getEmailNotificationsEnabled())
                .smsNotificationsEnabled(user.getSmsNotificationsEnabled())
                .pushNotificationsEnabled(user.getPushNotificationsEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static class StaffWorkloadStats {
        private UUID staffId;
        private String name;
        private Long activeAssignmentCount;
        private Boolean isOverloaded;
        private LocalDateTime lastAssignmentTime;

        public StaffWorkloadStats() {}

        public StaffWorkloadStats(UUID staffId, String name, Long activeAssignmentCount, 
                                  Boolean isOverloaded, LocalDateTime lastAssignmentTime) {
            this.staffId = staffId;
            this.name = name;
            this.activeAssignmentCount = activeAssignmentCount;
            this.isOverloaded = isOverloaded;
            this.lastAssignmentTime = lastAssignmentTime;
        }

        public UUID getStaffId() { return staffId; }
        public void setStaffId(UUID staffId) { this.staffId = staffId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getActiveAssignmentCount() { return activeAssignmentCount; }
        public void setActiveAssignmentCount(Long activeAssignmentCount) { this.activeAssignmentCount = activeAssignmentCount; }
        public Boolean getIsOverloaded() { return isOverloaded; }
        public void setIsOverloaded(Boolean isOverloaded) { this.isOverloaded = isOverloaded; }
        public LocalDateTime getLastAssignmentTime() { return lastAssignmentTime; }
        public void setLastAssignmentTime(LocalDateTime lastAssignmentTime) { this.lastAssignmentTime = lastAssignmentTime; }

        public static StaffWorkloadStatsBuilder builder() {
            return new StaffWorkloadStatsBuilder();
        }

        public static class StaffWorkloadStatsBuilder {
            private UUID staffId;
            private String name;
            private Long activeAssignmentCount;
            private Boolean isOverloaded;
            private LocalDateTime lastAssignmentTime;

            public StaffWorkloadStatsBuilder staffId(UUID staffId) {
                this.staffId = staffId;
                return this;
            }

            public StaffWorkloadStatsBuilder name(String name) {
                this.name = name;
                return this;
            }

            public StaffWorkloadStatsBuilder activeAssignmentCount(Long activeAssignmentCount) {
                this.activeAssignmentCount = activeAssignmentCount;
                return this;
            }

            public StaffWorkloadStatsBuilder isOverloaded(Boolean isOverloaded) {
                this.isOverloaded = isOverloaded;
                return this;
            }

            public StaffWorkloadStatsBuilder lastAssignmentTime(LocalDateTime lastAssignmentTime) {
                this.lastAssignmentTime = lastAssignmentTime;
                return this;
            }

            public StaffWorkloadStats build() {
                return new StaffWorkloadStats(staffId, name, activeAssignmentCount, isOverloaded, lastAssignmentTime);
            }
        }
    }
}
