package org.example.doansummer2026.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Mock SMS service để test local không tốn tiền.
 * Bật bằng cách: app.sms.provider=mock
 */
@Service
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "mock")
public class MockSmsService implements SmsService {
    @Override
    public void sendOtp(String phone, String code) {
        // Log ra console để test
        System.out.println("[MOCK SMS] Gửi OTP " + code + " đến " + phone);
    }
}