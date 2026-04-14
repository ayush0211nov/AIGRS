package com.aigrs.backend.repository;

import com.aigrs.backend.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findByCode(String code);
    Optional<Organization> findByIdAndIsActiveTrue(UUID id);
    boolean existsByCode(String code);
}
