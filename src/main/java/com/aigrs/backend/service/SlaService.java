package com.aigrs.backend.service;

import com.aigrs.backend.entity.Grievance;
import com.aigrs.backend.entity.Organization;
import com.aigrs.backend.entity.SlaEvent;
import com.aigrs.backend.enums.GrievanceStatus;
import com.aigrs.backend.enums.Priority;
import com.aigrs.backend.enums.SlaEventType;
import com.aigrs.backend.repository.GrievanceRepository;
import com.aigrs.backend.repository.OrganizationRepository;
import com.aigrs.backend.repository.SlaEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlaService {

    private final SlaEventRepository slaEventRepository;
    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;

    /** Default SLA hours per priority if org has no custom config */
    private static final Map<Priority, Integer> DEFAULT_SLA_HOURS = Map.of(
            Priority.LOW, 72,
            Priority.MEDIUM, 48,
            Priority.HIGH, 24,
            Priority.CRITICAL, 4
    );

    /**
     * Calculates SLA deadline based on org config or defaults,
     * then creates a STARTED SlaEvent.
     */
    public LocalDateTime calculateAndStartSla(Grievance grievance) {
        int slaHours = getSlaHoursForPriority(grievance.getOrgId(), grievance.getPriority());
        LocalDateTime deadline = LocalDateTime.now().plusHours(slaHours);

        SlaEvent event = SlaEvent.builder()
                .grievanceId(grievance.getId())
                .eventType(SlaEventType.STARTED)
                .build();
        event.setOrgId(grievance.getOrgId());
        slaEventRepository.save(event);

        return deadline;
    }

    /** Pause SLA when grievance goes ON_HOLD */
    public void pauseSla(UUID grievanceId, UUID orgId) {
        SlaEvent event = SlaEvent.builder()
                .grievanceId(grievanceId)
                .eventType(SlaEventType.PAUSED)
                .pausedAt(LocalDateTime.now())
                .build();
        event.setOrgId(orgId);
        slaEventRepository.save(event);
    }

    /** Resume SLA, calculate pause duration, extend deadline */
    public LocalDateTime resumeSla(Grievance grievance) {
        var lastPause = slaEventRepository
                .findTopByGrievanceIdAndEventTypeOrderByCreatedAtDesc(grievance.getId(), SlaEventType.PAUSED);

        long pausedMinutes = 0;
        if (lastPause.isPresent() && lastPause.get().getResumedAt() == null) {
            LocalDateTime pausedAt = lastPause.get().getPausedAt();
            pausedMinutes = java.time.Duration.between(pausedAt, LocalDateTime.now()).toMinutes();

            // Update pause event with resume time and duration
            SlaEvent pause = lastPause.get();
            pause.setResumedAt(LocalDateTime.now());
            pause.setPausedDurationMinutes(pausedMinutes);
            slaEventRepository.save(pause);
        }

        // Create RESUMED event
        SlaEvent resumeEvent = SlaEvent.builder()
                .grievanceId(grievance.getId())
                .eventType(SlaEventType.RESUMED)
                .build();
        resumeEvent.setOrgId(grievance.getOrgId());
        slaEventRepository.save(resumeEvent);

        // Extend deadline by pause duration
        if (grievance.getSlaDeadline() != null && pausedMinutes > 0) {
            return grievance.getSlaDeadline().plusMinutes(pausedMinutes);
        }
        return grievance.getSlaDeadline();
    }

    public int getSlaHoursForPriority(UUID orgId, Priority priority) {
        try {
            Organization org = organizationRepository.findById(orgId).orElse(null);
            if (org != null && org.getSlaConfig() != null) {
                Map<String, Integer> config = objectMapper.readValue(
                        org.getSlaConfig(), new TypeReference<>() {});
                Integer hours = config.get(priority.name());
                if (hours != null) return hours;
            }
        } catch (Exception e) {
            log.warn("Failed to parse SLA config for org {}, using defaults", orgId);
        }
        return DEFAULT_SLA_HOURS.getOrDefault(priority, 48);
    }
}
