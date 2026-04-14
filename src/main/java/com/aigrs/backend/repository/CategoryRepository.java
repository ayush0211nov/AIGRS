package com.aigrs.backend.repository;

import com.aigrs.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByOrgIdAndIsActiveTrue(UUID orgId);
    Optional<Category> findByIdAndOrgId(UUID id, UUID orgId);
    boolean existsByNameAndOrgId(String name, UUID orgId);
}
