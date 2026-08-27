package org.example.doansummer2026.service;

/**
 * Giao diện gửi SMS - có thể thay đổi gateway dễ dàng.
 */
public interface SmsService {
    void sendOtp(String phone, String code);
}



