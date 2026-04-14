package com.aigrs.backend.repository;

import com.aigrs.backend.entity.User;
import com.aigrs.backend.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhoneAndOrgId(String phone, UUID orgId);
    Optional<User> findByEmailAndOrgId(String email, UUID orgId);
    Optional<User> findByIdAndOrgId(UUID id, UUID orgId);
    boolean existsByPhoneAndOrgId(String phone, UUID orgId);
    boolean existsByEmailAndOrgId(String email, UUID orgId);
    Page<User> findByOrgIdAndRole(UUID orgId, UserRole role, Pageable pageable);
    List<User> findByOrgIdAndRoleIn(UUID orgId, List<UserRole> roles);
    List<User> findByOrgIdAndIsActiveAndRoleIn(UUID orgId, boolean isActive, List<UserRole> roles);
    List<User> findByOrgIdAndDepartmentIdAndIsActiveAndRoleIn(UUID orgId, UUID departmentId, boolean isActive, List<UserRole> roles);

    long countByOrgId(UUID orgId);
}
