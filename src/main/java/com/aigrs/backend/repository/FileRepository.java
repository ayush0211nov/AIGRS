package com.aigrs.backend.repository;

import com.aigrs.backend.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {
    Optional<FileEntity> findByIdAndOrgIdAndDeletedAtIsNull(UUID id, UUID orgId);
}
