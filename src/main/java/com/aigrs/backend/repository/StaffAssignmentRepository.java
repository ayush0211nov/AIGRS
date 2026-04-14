package com.aigrs.backend.repository;

import com.aigrs.backend.entity.StaffAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, UUID> {
    Optional<StaffAssignment> findByGrievanceIdAndIsActiveTrue(UUID grievanceId);
    List<StaffAssignment> findByStaffIdAndIsActiveTrueAndOrgId(UUID staffId, UUID orgId);
    List<StaffAssignment> findByStaffIdAndIsActiveAndOrgId(UUID staffId, boolean isActive, UUID orgId);
    long countByStaffIdAndIsActiveTrueAndOrgId(UUID staffId, UUID orgId);
    long countByStaffIdAndIsActive(UUID staffId, boolean isActive);
}
