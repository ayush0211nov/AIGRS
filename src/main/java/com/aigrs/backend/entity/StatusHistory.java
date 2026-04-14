package com.aigrs.backend.entity;

import com.aigrs.backend.enums.GrievanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusHistory extends BaseEntity {

    @Column(name = "grievance_id", nullable = false)
    private UUID grievanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private GrievanceStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private GrievanceStatus toStatus;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grievance_id", insertable = false, updatable = false)
    private Grievance grievance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", insertable = false, updatable = false)
    private User changedByUser;
}
