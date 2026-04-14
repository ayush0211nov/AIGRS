package com.aigrs.backend.scheduler;

import com.aigrs.backend.entity.Grievance;
import com.aigrs.backend.entity.SlaEvent;
import com.aigrs.backend.enums.GrievanceStatus;
import com.aigrs.backend.enums.SlaEventType;
import com.aigrs.backend.repository.GrievanceRepository;
import com.aigrs.backend.repository.SlaEventRepository;
import com.aigrs.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlaScheduler {

    private final GrievanceRepository grievanceRepository;
    private final SlaEventRepository slaEventRepository;
    private final NotificationService notificationService;

    private static final List<GrievanceStatus> EXCLUDED_STATUSES = List.of(
            GrievanceStatus.RESOLVED,
            GrievanceStatus.REJECTED
    );

    /**
     * Runs every 60 seconds — checks for SLA breaches.
     * For each breached grievance:
     *  1. Creates BREACHED SlaEvent (if not already recorded)
     *  2. Triggers notifications to staff + supervisor + admin
     */
    @Scheduled(fixedRate = 60000)
    public void checkSlaBreaches() {
        LocalDateTime now = LocalDateTime.now();
        List<Grievance> breached = grievanceRepository.findBreachedGrievances(EXCLUDED_STATUSES, now);

        for (Grievance grievance : breached) {
            // Skip if already marked as breached
            if (slaEventRepository.existsByGrievanceIdAndEventType(grievance.getId(), SlaEventType.BREACHED)) {
                continue;
            }

            log.warn("SLA BREACHED for grievance {} (tracking: {}, deadline was: {})",
                    grievance.getId(), grievance.getTrackingId(), grievance.getSlaDeadline());

            // Record breach event
            SlaEvent event = SlaEvent.builder()
                    .grievanceId(grievance.getId())
                    .eventType(SlaEventType.BREACHED)
                    .build();
            event.setOrgId(grievance.getOrgId());
            slaEventRepository.save(event);

            // Notify stakeholders
            try {
                notificationService.onSlaBreach(grievance);
            } catch (Exception e) {
                log.error("Failed to send SLA breach notification for {}: {}", grievance.getTrackingId(), e.getMessage());
            }
        }

        if (!breached.isEmpty()) {
            log.info("SLA breach check completed: {} grievances processed", breached.size());
        }
    }

    /**
     * Runs every 5 minutes — checks for grievances approaching SLA deadline.
     * Warns when remaining time is less than 25% of total SLA duration.
     */
    @Scheduled(fixedRate = 300000)
    public void checkSlaWarnings() {
        LocalDateTime now = LocalDateTime.now();
        // Look ahead 6 hours as a reasonable warning window
        LocalDateTime warningThreshold = now.plusHours(6);

        List<Grievance> approaching = grievanceRepository.findApproachingSla(EXCLUDED_STATUSES, now, warningThreshold);

        for (Grievance grievance : approaching) {
            // Skip if warning already sent
            if (slaEventRepository.existsByGrievanceIdAndEventType(grievance.getId(), SlaEventType.WARNING)) {
                continue;
            }

            log.info("SLA WARNING for grievance {} (tracking: {}, deadline: {})",
                    grievance.getId(), grievance.getTrackingId(), grievance.getSlaDeadline());

            SlaEvent event = SlaEvent.builder()
                    .grievanceId(grievance.getId())
                    .eventType(SlaEventType.WARNING)
                    .build();
            event.setOrgId(grievance.getOrgId());
            slaEventRepository.save(event);

            try {
                notificationService.onSlaWarning(grievance);
            } catch (Exception e) {
                log.error("Failed to send SLA warning for {}: {}", grievance.getTrackingId(), e.getMessage());
            }
        }
    }
}
