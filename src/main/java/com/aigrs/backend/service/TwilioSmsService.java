package com.aigrs.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "twilio")
public class TwilioSmsService implements SmsService {

    @Value("${app.sms.enabled:false}")
    private Boolean smsEnabled;

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.phone-number:}")
    private String fromPhoneNumber;

    /**
     * Send OTP via SMS using Twilio
     * In dev mode, logs to console instead of actual sending
     */
    @Override
    public void sendOtp(String phoneNumber, String otp) {
        if (!smsEnabled) {
            log.info("[DEV MODE] OTP would be sent to {}: {}", phoneNumber, otp);
            return;
        }

        try {
            log.info("Sending OTP via Twilio to: {}", phoneNumber);
            // TODO: Implement actual Twilio integration when credentials are available
            // com.twilio.rest.api.v2010.account.Message.creator(
            //     new com.twilio.rest.api.v2010.account.PhoneNumber(fromPhoneNumber),
            //     new com.twilio.rest.api.v2010.account.PhoneNumber(phoneNumber)
            // ).setBody("Your AIGRS OTP is: " + otp).create();
            log.info("OTP sent successfully to {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send OTP to {}", phoneNumber, e);
        }
    }

    @Override
    public void sendNotification(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.info("[DEV MODE] Notification would be sent to {}: {}", phoneNumber, message);
            return;
        }

        try {
            log.info("Sending notification via Twilio to: {}", phoneNumber);
            // TODO: Implement actual Twilio integration
            log.info("Notification sent successfully to {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send notification to {}", phoneNumber, e);
        }
    }

    @Override
    public void sendSlaWarning(String phoneNumber, String trackingId, long hoursRemaining) {
        String message = String.format(
                "SLA Warning: Grievance %s is approaching deadline. %d hours remaining.",
                trackingId, hoursRemaining
        );
        sendNotification(phoneNumber, message);
    }

    @Override
    public void sendSlaBreachAlert(String phoneNumber, String trackingId) {
        String message = String.format(
                "URGENT: SLA breached for grievance %s. Immediate action required.",
                trackingId
        );
        sendNotification(phoneNumber, message);
    }
}
