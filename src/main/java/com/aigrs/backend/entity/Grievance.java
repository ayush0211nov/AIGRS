package com.aigrs.backend.entity;

import com.aigrs.backend.enums.GrievanceStatus;
import com.aigrs.backend.enums.Priority;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "grievances", indexes = {
        @Index(name = "idx_grievance_org", columnList = "org_id"),
        @Index(name = "idx_grievance_status", columnList = "status"),
        @Index(name = "idx_grievance_submitter", columnList = "submitter_id"),
        @Index(name = "idx_grievance_tracking", columnList = "tracking_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grievance extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Human-readable ID: {ORG_CODE}-{YEAR}-{6-digit-seq} */
    @Column(name = "tracking_id", nullable = false, unique = true)
    private String trackingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GrievanceStatus status = GrievanceStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "submitter_id", nullable = false)
    private UUID submitterId;

    @Column(name = "assigned_staff_id")
    private UUID assignedStaffId;

    private String location;

    private Double latitude;

    private Double longitude;

    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_duplicate")
    @Builder.Default
    private Boolean isDuplicate = false;

    @Column(name = "duplicate_of_id")
    private UUID duplicateOfId;

    @Column(name = "ai_sentiment")
    private String aiSentiment;

    @Column(name = "estimated_hours")
    private Double estimatedHours;

    /** Comma-separated file IDs for attachments */
    @Column(name = "attachment_ids", columnDefinition = "TEXT")
    private String attachmentIds;

    // ---- Relationships ----

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitter_id", insertable = false, updatable = false)
    private User submitter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id", insertable = false, updatable = false)
    private User assignedStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", insertable = false, updatable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", insertable = false, updatable = false)
    private Organization organization;

    @OneToMany(mappedBy = "grievance", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StatusHistory> statusHistories = new ArrayList<>();

    @OneToMany(mappedBy = "grievance", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "grievance", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Rating> ratings = new ArrayList<>();
}
