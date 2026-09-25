package vn.edu.fpt.cares.service;

/**
 * Giao diện gửi SMS - có thể thay đổi gateway dễ dàng.
 */
public interface SmsService {
    void sendOtp(String phone, String code);
}



