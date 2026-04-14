package com.aigrs.backend.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Send a simple text email
     */
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@aigrs.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
        }
    }

    /**
     * Send HTML email with Thymeleaf template
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@aigrs.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("HTML email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}", to, e);
        }
    }

    /**
     * Send notification email
     */
    public void sendNotificationEmail(String to, String grievanceId, String trackingId, String status) {
        String subject = "Grievance Status Update - " + trackingId;
        String body = String.format(
                "Your grievance %s status has been updated to: %s\n\n" +
                "View details: https://aigrs.local/grievances/%s",
                trackingId, status, grievanceId
        );
        sendSimpleEmail(to, subject, body);
    }

    /**
     * Send OTP email
     */
    public void sendOtpEmail(String to, String otp) {
        String subject = "Your AIGRS OTP";
        String body = String.format(
                "Your OTP for AIGRS is: %s\n\n" +
                "This OTP is valid for 10 minutes. Do not share it with anyone.",
                otp
        );
        sendSimpleEmail(to, subject, body);
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String to, String resetToken) {
        String subject = "Password Reset Request - AIGRS";
        String resetLink = "https://aigrs.local/reset-password?token=" + resetToken;
        String body = String.format(
                "Click the link below to reset your password:\n%s\n\n" +
                "This link is valid for 24 hours.\n\n" +
                "If you didn't request this, please ignore this email.",
                resetLink
        );
        sendSimpleEmail(to, subject, body);
    }

    /**
     * Send grievance comment notification
     */
    public void sendCommentNotification(String to, String trackingId, String userName) {
        String subject = "New Comment on Grievance - " + trackingId;
        String body = String.format(
                "User %s has added a comment to grievance %s.\n\n" +
                "View grievance: https://aigrs.local/grievances/%s",
                userName, trackingId, trackingId
        );
        sendSimpleEmail(to, subject, body);
    }

    /**
     * Send SLA warning email
     */
    public void sendSlaWarningEmail(String to, String trackingId, long hoursRemaining) {
        String subject = "SLA Warning - Grievance " + trackingId;
        String body = String.format(
                "This is a reminder that grievance %s is approaching its SLA deadline.\n\n" +
                "Hours remaining: %d\n\n" +
                "Please take immediate action to resolve this grievance.",
                trackingId, hoursRemaining
        );
        sendSimpleEmail(to, subject, body);
    }

    /**
     * Send SLA breach email
     */
    public void sendSlaBreachEmail(String to, String trackingId) {
        String subject = "SLA Breach Alert - Grievance " + trackingId;
        String body = String.format(
                "URGENT: SLA has been breached for grievance %s.\n\n" +
                "Please take immediate action to resolve this grievance.",
                trackingId
        );
        sendSimpleEmail(to, subject, body);
    }
}
