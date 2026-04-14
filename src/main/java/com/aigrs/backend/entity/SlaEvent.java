package com.aigrs.backend.entity;

import com.aigrs.backend.enums.SlaEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sla_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaEvent extends BaseEntity {

    @Column(name = "grievance_id", nullable = false)
    private UUID grievanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private SlaEventType eventType;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "resumed_at")
    private LocalDateTime resumedAt;

    /** Total minutes SLA was paused for this event */
    @Column(name = "paused_duration_minutes")
    private Long pausedDurationMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grievance_id", insertable = false, updatable = false)
    private Grievance grievance;
}
