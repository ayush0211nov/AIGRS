package com.aigrs.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /** Short unique code used in grievance tracking IDs, e.g. "MUN" */
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "logo_url")
    private String logoUrl;

    private String address;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** JSON blob storing SLA hours per priority, e.g. {"LOW":72,"MEDIUM":48,"HIGH":24,"CRITICAL":4} */
    @Column(name = "sla_config", columnDefinition = "TEXT")
    private String slaConfig;

    /** JSON blob storing escalation rules */
    @Column(name = "escalation_config", columnDefinition = "TEXT")
    private String escalationConfig;
}
