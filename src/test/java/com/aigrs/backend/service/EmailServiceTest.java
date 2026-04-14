package com.aigrs.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private String testEmail = "test@example.com";

    @Test
    void testSendSimpleEmail() {
        // Arrange & Act
        assertDoesNotThrow(() -> emailService.sendSimpleEmail(testEmail, "Test Subject", "Test Body"));

        // Assert
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendOtpEmail() {
        // Arrange & Act
        assertDoesNotThrow(() -> emailService.sendOtpEmail(testEmail, "123456"));

        // Assert
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendNotificationEmail() {
        // Arrange & Act
        assertDoesNotThrow(() -> emailService.sendNotificationEmail(testEmail, "test-id", "GRV-2024-001", "SUBMITTED"));

        // Assert
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendPasswordResetEmail() {
        // Arrange & Act
        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail(testEmail, "reset-token-123"));

        // Assert
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendSlaWarningEmail() {
        // Arrange & Act
        assertDoesNotThrow(() -> emailService.sendSlaWarningEmail(testEmail, "GRV-2024-001", 4L));

        // Assert
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendSlaBreachEmail() {
        // Arrange & Act
        assertDoesNotThrow(() -> emailService.sendSlaBreachEmail(testEmail, "GRV-2024-001"));

        // Assert
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
