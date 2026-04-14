package com.aigrs.backend.repository;

import com.aigrs.backend.entity.SlaEvent;
import com.aigrs.backend.enums.SlaEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlaEventRepository extends JpaRepository<SlaEvent, UUID> {
    List<SlaEvent> findByGrievanceIdOrderByCreatedAtAsc(UUID grievanceId);
    Optional<SlaEvent> findTopByGrievanceIdAndEventTypeOrderByCreatedAtDesc(UUID grievanceId, SlaEventType eventType);
    boolean existsByGrievanceIdAndEventType(UUID grievanceId, SlaEventType eventType);
}
