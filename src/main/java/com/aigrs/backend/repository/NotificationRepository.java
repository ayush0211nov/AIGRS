package com.aigrs.backend.repository;

import com.aigrs.backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserIdAndOrgIdOrderByCreatedAtDesc(UUID userId, UUID orgId, Pageable pageable);
    List<Notification> findByUserIdAndOrgId(UUID userId, UUID orgId);
    long countByUserIdAndOrgIdAndIsReadFalse(UUID userId, UUID orgId);
    Optional<Notification> findByIdAndUserIdAndOrgId(UUID id, UUID userId, UUID orgId);
}
