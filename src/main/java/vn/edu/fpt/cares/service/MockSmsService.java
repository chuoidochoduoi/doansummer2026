package vn.edu.fpt.cares.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Mock SMS service để test local không tốn tiền.
 * Bật bằng cách: app.sms.provider=mock
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "mock")
public class MockSmsService implements SmsService {
    @Override
    public void sendOtp(String phone, String code) {
        String suffix = phone == null || phone.length() < 3
                ? "***"
                : "***" + phone.substring(phone.length() - 3);
        log.info("Đã giả lập gửi OTP tới số điện thoại {}", suffix);
    }
}



