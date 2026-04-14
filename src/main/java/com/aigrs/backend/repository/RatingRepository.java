package com.aigrs.backend.repository;

import com.aigrs.backend.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {
    Optional<Rating> findByGrievanceIdAndUserId(UUID grievanceId, UUID userId);
    Optional<Rating> findByGrievanceIdAndOrgId(UUID grievanceId, UUID orgId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.orgId = :orgId")
    Double avgRatingByOrg(@Param("orgId") UUID orgId);

    @Query("SELECT AVG(r.score) FROM Rating r JOIN Grievance g ON r.grievanceId = g.id " +
            "WHERE g.assignedStaffId = :staffId AND r.orgId = :orgId")
    Double avgRatingByStaff(@Param("staffId") UUID staffId, @Param("orgId") UUID orgId);
}
