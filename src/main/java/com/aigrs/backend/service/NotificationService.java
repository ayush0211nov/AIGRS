package com.aigrs.backend.service;

import com.aigrs.backend.entity.Comment;
import com.aigrs.backend.entity.Grievance;
import com.aigrs.backend.entity.Notification;
import com.aigrs.backend.entity.User;
import com.aigrs.backend.enums.GrievanceStatus;
import com.aigrs.backend.enums.NotificationType;
import com.aigrs.backend.repository.GrievanceRepository;
import com.aigrs.backend.repository.NotificationRepository;
import com.aigrs.backend.repository.UserRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    // ---- Event Handlers ----

    public void onGrievanceSubmitted(Grievance grievance) {
        createAndSend(
                grievance.getSubmitterId(),
                grievance.getOrgId(),
                NotificationType.STATUS_CHANGE,
                "Grievance Submitted",
                "Your grievance " + grievance.getTrackingId() + " has been submitted successfully.",
                grievance.getId()
        );

        if (grievance.getAssignedStaffId() != null) {
            onGrievanceAssigned(grievance);
        }
    }

    public void onGrievanceAssigned(Grievance grievance) {
        if (grievance.getAssignedStaffId() == null) return;
        createAndSend(
                grievance.getAssignedStaffId(),
                grievance.getOrgId(),
                NotificationType.ASSIGNMENT,
                "New Grievance Assigned",
                "Grievance " + grievance.getTrackingId() + " has been assigned to you.",
                grievance.getId()
        );
    }

    public void onStatusChanged(Grievance grievance, GrievanceStatus oldStatus, GrievanceStatus newStatus) {
        createAndSend(
                grievance.getSubmitterId(),
                grievance.getOrgId(),
                NotificationType.STATUS_CHANGE,
                "Grievance Status Updated",
                "Your grievance " + grievance.getTrackingId() + " status changed from " +
                        oldStatus + " to " + newStatus + ".",
                grievance.getId()
        );
    }

    public void onCommentAdded(UUID grievanceId, Comment comment) {
        // Notify grievance submitter (unless they wrote the comment)
        // We'd need to fetch the grievance — simplified here
        log.info("Comment notification triggered for grievance {}", grievanceId);
    }

    public void onRatingReceived(Grievance grievance, int score) {
        if (grievance.getAssignedStaffId() != null) {
            createAndSend(
                    grievance.getAssignedStaffId(),
                    grievance.getOrgId(),
                    NotificationType.RATING,
                    "New Rating Received",
                    "You received a " + score + "/5 rating for grievance " + grievance.getTrackingId(),
                    grievance.getId()
            );
        }
    }

    public void onSlaWarning(Grievance grievance) {
        if (grievance.getAssignedStaffId() != null) {
            createAndSend(
                    grievance.getAssignedStaffId(),
                    grievance.getOrgId(),
                    NotificationType.SLA_WARNING,
                    "SLA Warning",
                    "Grievance " + grievance.getTrackingId() + " is approaching its SLA deadline.",
                    grievance.getId()
            );
        }
    }

    public void onSlaBreach(Grievance grievance) {
        if (grievance.getAssignedStaffId() != null) {
            createAndSend(
                    grievance.getAssignedStaffId(),
                    grievance.getOrgId(),
                    NotificationType.SLA_BREACH,
                    "SLA Breached",
                    "Grievance " + grievance.getTrackingId() + " has breached its SLA deadline!",
                    grievance.getId()
            );
        }
    }

    // ---- Core Methods ----

    private void createAndSend(UUID userId, UUID orgId, NotificationType type,
                                String title, String body, UUID grievanceId) {
        // Persist in-app notification
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .grievanceId(grievanceId)
                .build();
        notification.setOrgId(orgId);
        notificationRepository.save(notification);

        // Send push notification
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getPushNotificationsEnabled() && user.getFcmToken() != null) {
                sendPushNotification(user.getFcmToken(), title, body, Map.of("grievanceId", grievanceId.toString()));
            }
            if (user.getEmailNotificationsEnabled() && user.getEmail() != null) {
                sendEmail(user.getEmail(), title, body);
            }
        });
    }

    public void sendPushNotification(String fcmToken, String title, String body, Map<String, String> data) {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                log.debug("Firebase not initialized, skipping push notification");
                return;
            }
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.debug("FCM message sent: {}", response);
        } catch (Exception e) {
            log.warn("Failed to send FCM push: {}", e.getMessage());
        }
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject("[AIGRS] " + subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
