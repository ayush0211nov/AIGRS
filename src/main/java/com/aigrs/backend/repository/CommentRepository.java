package com.aigrs.backend.repository;

import com.aigrs.backend.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Page<Comment> findByGrievanceIdAndOrgIdOrderByCreatedAtDesc(UUID grievanceId, UUID orgId, Pageable pageable);
    Page<Comment> findByGrievanceIdAndOrgIdAndIsInternalFalseOrderByCreatedAtDesc(UUID grievanceId, UUID orgId, Pageable pageable);
}
