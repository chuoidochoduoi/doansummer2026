package org.example.doansummer2026.service;

import org.example.doansummer2026.exception.BadRequestException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

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
    private static final String SEND_COOLDOWN_PREFIX = "otp:cooldown:";
    private static final String SEND_COUNT_PREFIX = "otp:send-count:";
    private static final String VERIFY_COUNT_PREFIX = "otp:verify-count:";

    private final StringRedisTemplate redisTemplate;
    private final SmsService smsService;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.otp.cooldown-seconds:60}")
    private long cooldownSeconds;
    @Value("${app.otp.max-sends-per-hour:5}")
    private long maxSendsPerHour;
    @Value("${app.otp.max-verify-attempts:5}")
    private long maxVerifyAttempts;

    public OtpService(StringRedisTemplate redisTemplate, SmsService smsService, EmailService emailService) {
        this.redisTemplate = redisTemplate;
        this.smsService = smsService;
        this.emailService = emailService;
    }

    /** Sinh OTP cho SĐT hoac Email, lưu vào Redis, và gửi đi */
    public String sendOtp(String identifier) {
        identifier = normalize(identifier);
        String cooldownKey = SEND_COOLDOWN_PREFIX + identifier;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            Long ttl = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
            throw new BadRequestException("Vui long cho " + Math.max(ttl == null ? 1 : ttl, 1) + " giay truoc khi gui lai OTP");
        }
        String sendCountKey = SEND_COUNT_PREFIX + identifier;
        Long sendCount = redisTemplate.opsForValue().increment(sendCountKey);
        if (sendCount != null && sendCount == 1) {
            redisTemplate.expire(sendCountKey, 1, TimeUnit.HOURS);
        }
        if (sendCount != null && sendCount > maxSendsPerHour) {
            throw new BadRequestException("Da vuot qua so lan gui OTP cho phep trong mot gio");
        }

        String key = OTP_PREFIX + identifier;
        String previousCode = redisTemplate.opsForValue().get(key);
        String code;
        do {
            code = String.valueOf(random.nextInt(900_000) + 100_000);
        } while (code.equals(previousCode));

        redisTemplate.opsForValue().set(key, code, TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(cooldownKey, "1", cooldownSeconds, TimeUnit.SECONDS);
        redisTemplate.delete(VERIFY_COUNT_PREFIX + identifier);

        if (identifier.contains("@")) {
            emailService.sendOtpEmail(identifier, code);
        } else {
            smsService.sendOtp(identifier, code);
        }
        return code;
    }

    /** Trả về true nếu OTP hợp lệ và đã bị tiêu thụ (one-time use) */
    public boolean verifyOtp(String identifier, String code) {
        identifier = normalize(identifier);
        String key = OTP_PREFIX + identifier;
        String verifyCountKey = VERIFY_COUNT_PREFIX + identifier;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            return false;
        }
        if (!code.equals(storedCode)) {
            Long attempts = redisTemplate.opsForValue().increment(verifyCountKey);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(verifyCountKey, TTL_MINUTES, TimeUnit.MINUTES);
            }
            if (attempts != null && attempts >= maxVerifyAttempts) {
                redisTemplate.delete(key);
                throw new BadRequestException("Da nhap sai OTP qua so lan cho phep; vui long gui ma moi");
            }
            throw new BadRequestException("Ma otp khong chinh xac");
        }
        // One-time: xóa luôn sau khi dùng
        redisTemplate.delete(key);
        redisTemplate.delete(verifyCountKey);
        return true;
    }

    private String normalize(String identifier) {
        return identifier == null ? "" : identifier.trim().toLowerCase();
    }
}



