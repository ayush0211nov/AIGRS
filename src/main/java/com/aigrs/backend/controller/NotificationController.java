package com.aigrs.backend.controller;

import com.aigrs.backend.dto.response.ApiResponse;
import com.aigrs.backend.entity.Notification;
import com.aigrs.backend.exception.ResourceNotFoundException;
import com.aigrs.backend.repository.NotificationRepository;
import com.aigrs.backend.repository.UserRepository;
import com.aigrs.backend.util.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification management")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get user notifications (unread_count in X-Unread-Count header)")
    public ResponseEntity<ApiResponse<Page<Notification>>> list(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UUID orgId = TenantContext.getOrgUUID();
        Page<Notification> page = notificationRepository.findByUserIdAndOrgIdOrderByCreatedAtDesc(userId, orgId, pageable);
        long unreadCount = notificationRepository.countByUserIdAndOrgIdAndIsReadFalse(userId, orgId);

        return ResponseEntity.ok()
                .header("X-Unread-Count", String.valueOf(unreadCount))
                .body(ApiResponse.success(page));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable UUID id, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UUID orgId = TenantContext.getOrgUUID();
        Notification notification = notificationRepository.findByIdAndUserIdAndOrgId(id, userId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PatchMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read for current user")
    public ResponseEntity<ApiResponse<Void>> markAllRead(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UUID orgId = TenantContext.getOrgUUID();
        // Find all unread notifications for user and mark as read
        var unreadNotifications = notificationRepository.findByUserIdAndOrgId(userId, orgId);
        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update FCM token and notification preferences")
    public ResponseEntity<ApiResponse<Void>> updatePreferences(
            @RequestBody Map<String, Object> prefs,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UUID orgId = TenantContext.getOrgUUID();
        var user = userRepository.findByIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (prefs.containsKey("fcmToken")) {
            user.setFcmToken((String) prefs.get("fcmToken"));
        }
        if (prefs.containsKey("emailNotifications")) {
            user.setEmailNotificationsEnabled((Boolean) prefs.get("emailNotifications"));
        }
        if (prefs.containsKey("smsNotifications")) {
            user.setSmsNotificationsEnabled((Boolean) prefs.get("smsNotifications"));
        }
        if (prefs.containsKey("pushNotifications")) {
            user.setPushNotificationsEnabled((Boolean) prefs.get("pushNotifications"));
        }

        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated", null));
    }
}
