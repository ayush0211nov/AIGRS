package com.aigrs.backend.scheduler;

import com.aigrs.backend.entity.Organization;
import com.aigrs.backend.entity.User;
import com.aigrs.backend.enums.GrievanceStatus;
import com.aigrs.backend.enums.UserRole;
import com.aigrs.backend.repository.GrievanceRepository;
import com.aigrs.backend.repository.OrganizationRepository;
import com.aigrs.backend.repository.UserRepository;
import com.aigrs.backend.service.NotificationService;
import com.aigrs.backend.repository.StaffAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DigestScheduler {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final GrievanceRepository grievanceRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final NotificationService notificationService;

    /**
     * Sends a daily digest email at 9 AM every day.
     * For each staff member: pending count + list of grievances due today.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyDigest() {
        log.info("Starting daily digest job...");

        List<Organization> activeOrgs = organizationRepository.findAll().stream()
                .filter(Organization::getIsActive)
                .toList();

        for (Organization org : activeOrgs) {
            List<User> staff = userRepository.findByOrgIdAndRoleIn(
                    org.getId(), List.of(UserRole.STAFF, UserRole.SUPERVISOR));

            for (User user : staff) {
                long assignedCount = staffAssignmentRepository.countByStaffIdAndIsActiveTrueAndOrgId(
                        user.getId(), org.getId());

                if (assignedCount == 0) continue;

                String subject = "Daily Digest: " + assignedCount + " Active Grievances";
                String body = String.format(
                        "Good morning %s,\n\nYou have %d active grievance(s) assigned to you.\n\n" +
                                "Please log in to review and update their status.\n\n" +
                                "— AIGRS Notification System",
                        user.getName(), assignedCount
                );

                try {
                    if (user.getEmail() != null && user.getEmailNotificationsEnabled()) {
                        notificationService.sendEmail(user.getEmail(), subject, body);
                    }
                } catch (Exception e) {
                    log.warn("Failed to send digest to {}: {}", user.getEmail(), e.getMessage());
                }
            }
        }

        log.info("Daily digest job completed.");
    }
}
