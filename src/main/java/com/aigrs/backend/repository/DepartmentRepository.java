package com.aigrs.backend.repository;

import com.aigrs.backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    List<Department> findByOrgIdAndIsActiveTrue(UUID orgId);
    Optional<Department> findByIdAndOrgId(UUID id, UUID orgId);
}
