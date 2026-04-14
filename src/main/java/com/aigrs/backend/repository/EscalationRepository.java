package com.aigrs.backend.repository;

import com.aigrs.backend.entity.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EscalationRepository extends JpaRepository<Escalation, UUID> {
    List<Escalation> findByGrievanceIdOrderByCreatedAtAsc(UUID grievanceId);
}
