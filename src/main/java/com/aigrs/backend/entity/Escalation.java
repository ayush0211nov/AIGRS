package com.aigrs.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "escalations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Escalation extends BaseEntity {

    @Column(name = "grievance_id", nullable = false)
    private UUID grievanceId;

    /** User ID of the person the grievance was escalated from */
    @Column(name = "escalated_from")
    private UUID escalatedFrom;

    /** User ID of the person the grievance was escalated to */
    @Column(name = "escalated_to")
    private UUID escalatedTo;

    @Column(columnDefinition = "TEXT")
    private String reason;

    /** Escalation level (1, 2, 3...) */
    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grievance_id", insertable = false, updatable = false)
    private Grievance grievance;
}
