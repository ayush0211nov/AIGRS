package com.aigrs.backend.repository;

import com.aigrs.backend.entity.Grievance;
import com.aigrs.backend.enums.GrievanceStatus;
import com.aigrs.backend.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GrievanceRepository extends JpaRepository<Grievance, UUID>, JpaSpecificationExecutor<Grievance> {

    Optional<Grievance> findByIdAndOrgIdAndDeletedAtIsNull(UUID id, UUID orgId);
    Optional<Grievance> findByTrackingIdAndOrgId(String trackingId, UUID orgId);

    Page<Grievance> findByOrgIdAndDeletedAtIsNull(UUID orgId, Pageable pageable);
    Page<Grievance> findByOrgIdAndStatusAndDeletedAtIsNull(UUID orgId, GrievanceStatus status, Pageable pageable);
    Page<Grievance> findBySubmitterIdAndOrgIdAndDeletedAtIsNull(UUID submitterId, UUID orgId, Pageable pageable);
    Page<Grievance> findByAssignedStaffIdAndOrgIdAndDeletedAtIsNull(UUID staffId, UUID orgId, Pageable pageable);

    // Dashboard counts
    long countByOrgIdAndDeletedAtIsNull(UUID orgId);
    long countByOrgIdAndStatusAndDeletedAtIsNull(UUID orgId, GrievanceStatus status);

    @Query("SELECT COUNT(g) FROM Grievance g WHERE g.orgId = :orgId AND g.deletedAt IS NULL " +
            "AND g.status NOT IN ('RESOLVED', 'REJECTED') AND g.slaDeadline < :now")
    long countOverdue(@Param("orgId") UUID orgId, @Param("now") LocalDateTime now);

    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, g.createdAt, g.resolvedAt)) FROM Grievance g " +
            "WHERE g.orgId = :orgId AND g.resolvedAt IS NOT NULL AND g.deletedAt IS NULL")
    Double avgResolutionHours(@Param("orgId") UUID orgId);

    // Today's counts
    long countByOrgIdAndDeletedAtIsNullAndCreatedAtAfter(UUID orgId, LocalDateTime after);

    @Query("SELECT COUNT(g) FROM Grievance g WHERE g.orgId = :orgId AND g.deletedAt IS NULL " +
            "AND g.status = 'RESOLVED' AND g.resolvedAt >= :after")
    long countResolvedToday(@Param("orgId") UUID orgId, @Param("after") LocalDateTime after);

    // SLA checks
    @Query("SELECT g FROM Grievance g WHERE g.status NOT IN :excludedStatuses " +
            "AND g.slaDeadline IS NOT NULL AND g.slaDeadline < :now AND g.deletedAt IS NULL")
    List<Grievance> findBreachedGrievances(@Param("excludedStatuses") List<GrievanceStatus> excludedStatuses,
                                            @Param("now") LocalDateTime now);

    @Query("SELECT g FROM Grievance g WHERE g.status NOT IN :excludedStatuses " +
            "AND g.slaDeadline IS NOT NULL AND g.deletedAt IS NULL " +
            "AND g.slaDeadline > :now AND g.slaDeadline < :warningThreshold")
    List<Grievance> findApproachingSla(@Param("excludedStatuses") List<GrievanceStatus> excludedStatuses,
                                       @Param("now") LocalDateTime now,
                                       @Param("warningThreshold") LocalDateTime warningThreshold);

    // Chart data
    @Query("SELECT CAST(g.createdAt AS DATE) as day, COUNT(g) FROM Grievance g " +
            "WHERE g.orgId = :orgId AND g.deletedAt IS NULL AND g.createdAt >= :from " +
            "GROUP BY CAST(g.createdAt AS DATE) ORDER BY day")
    List<Object[]> dailySubmittedTrend(@Param("orgId") UUID orgId, @Param("from") LocalDateTime from);

    @Query("SELECT g.priority, COUNT(g) FROM Grievance g WHERE g.orgId = :orgId AND g.deletedAt IS NULL GROUP BY g.priority")
    List<Object[]> priorityBreakdown(@Param("orgId") UUID orgId);

    // Sequence generation
    @Query(value = "SELECT COUNT(*) FROM grievances WHERE org_id = :orgId AND EXTRACT(YEAR FROM created_at) = :year", nativeQuery = true)
    long countByOrgIdAndYear(@Param("orgId") UUID orgId, @Param("year") int year);

    // Full-text search
    @Query(value = "SELECT * FROM grievances g WHERE g.org_id = :orgId AND g.deleted_at IS NULL " +
            "AND to_tsvector('english', g.title || ' ' || g.description) @@ plainto_tsquery('english', :query)",
            countQuery = "SELECT COUNT(*) FROM grievances g WHERE g.org_id = :orgId AND g.deleted_at IS NULL " +
                    "AND to_tsvector('english', g.title || ' ' || g.description) @@ plainto_tsquery('english', :query)",
            nativeQuery = true)
    Page<Grievance> fullTextSearch(@Param("orgId") UUID orgId, @Param("query") String query, Pageable pageable);

    // Staff performance
    @Query("SELECT g.assignedStaffId, COUNT(g) FROM Grievance g WHERE g.orgId = :orgId AND g.deletedAt IS NULL " +
            "AND g.assignedStaffId IS NOT NULL GROUP BY g.assignedStaffId")
    List<Object[]> countByStaff(@Param("orgId") UUID orgId);

    // Heatmap data
    @Query("SELECT g.latitude, g.longitude, COUNT(g) FROM Grievance g " +
            "WHERE g.orgId = :orgId AND g.deletedAt IS NULL AND g.latitude IS NOT NULL AND g.longitude IS NOT NULL " +
            "GROUP BY g.latitude, g.longitude")
    List<Object[]> heatmapData(@Param("orgId") UUID orgId);
}
