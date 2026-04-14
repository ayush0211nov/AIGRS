package com.aigrs.backend.service;

public interface SmsService {
    
    /**
     * Send OTP via SMS
     */
    void sendOtp(String phoneNumber, String otp);
    
    /**
     * Send notification SMS
     */
    void sendNotification(String phoneNumber, String message);
    
    /**
     * Send SLA warning SMS
     */
    void sendSlaWarning(String phoneNumber, String trackingId, long hoursRemaining);
    
    /**
     * Send SLA breach SMS
     */
    void sendSlaBreachAlert(String phoneNumber, String trackingId);
}
