package com.aigrs.backend.repository;

import com.aigrs.backend.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {
    List<StatusHistory> findByGrievanceIdAndOrgIdOrderByCreatedAtAsc(UUID grievanceId, UUID orgId);
}
