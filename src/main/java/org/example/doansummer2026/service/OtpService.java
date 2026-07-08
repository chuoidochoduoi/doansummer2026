package org.example.doansummer2026.service;

import org.example.doansummer2026.exception.BadRequestException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Quản lý OTP theo số điện thoại sử dụng Redis + SMS gateway.
 * - Sinh code 6 chữ số, lưu kèm thời hạn 5 phút trong Redis.
 * - One-time use: xác thành công là xóa luôn.
 * - Gửi OTP qua SMS gateway được inject (Twilio, Zalo OA...).
 */
@Service
public class OtpService {

    private static final long TTL_MINUTES = 5;
    private static final String OTP_PREFIX = "otp:";

    private final StringRedisTemplate redisTemplate;
    private final SmsService smsService;
    private final SecureRandom random = new SecureRandom();

    public OtpService(StringRedisTemplate redisTemplate, SmsService smsService) {
        this.redisTemplate = redisTemplate;
        this.smsService = smsService;
    }

    /** Sinh OTP cho SĐT, lưu vào Redis, và gửi qua SMS gateway */
    public void sendOtp(String phone) {
        int code = random.nextInt(900_000) + 100_000;
        String key = OTP_PREFIX + phone;

        redisTemplate.opsForValue().set(key, String.valueOf(code), TTL_MINUTES, TimeUnit.MINUTES);

        smsService.sendOtp(phone, String.valueOf(code));
    }

    /** Trả về true nếu OTP hợp lệ và đã bị tiêu thụ (one-time use) */
    public boolean verifyOtp(String phone, String code) {
        String key = OTP_PREFIX + phone;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            return false;
        }
        if (!code.equals(storedCode)) {
            throw new BadRequestException("Ma otp khong chinh xac");
        }
        // One-time: xóa luôn sau khi dùng
        redisTemplate.delete(key);
        return true;
    }
}