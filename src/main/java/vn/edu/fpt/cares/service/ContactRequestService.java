package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.fpt.cares.dto.contact.ContactRequestCreateRequest;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactRequestService {

    private static final String RATE_LIMIT_PREFIX = "contact:send-count:";
    private static final int MAX_SENDS = 3;
    private static final long RATE_LIMIT_MINUTES = 10;

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    @Value("${app.contact.recipient-email:}")
    private String recipientEmail;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.brevo.api-key:}")
    private String brevoApiKey;

    @Value("${app.brevo.sender-email:${spring.mail.username:}}")
    private String brevoSenderEmail;

    public void send(ContactRequestCreateRequest request) {
        String recipient = normalizeOptional(recipientEmail);
        boolean brevoConfigured = normalizeOptional(brevoApiKey) != null
                && normalizeOptional(brevoSenderEmail) != null;
        boolean smtpConfigured = normalizeOptional(mailUsername) != null
                && normalizeOptional(mailPassword) != null;
        if (recipient == null || (!brevoConfigured && !smtpConfigured)) {
            throw new ServiceUnavailableException("Kênh liên hệ qua email chưa được cấu hình");
        }

        String fullName = request.fullName().trim();
        String phone = request.phone().trim();
        String customerEmail = normalizeOptional(request.email());
        String subject = request.subject().trim();
        String message = request.message().trim();
        validateNormalized(fullName, subject, message);

        String rateLimitKey = RATE_LIMIT_PREFIX + sha256(phone);
        incrementRateLimit(rateLimitKey);

        try {
            emailService.sendContactEmail(
                    recipient,
                    fullName,
                    phone,
                    customerEmail,
                    subject,
                    message);
            log.info("Đã gửi email liên hệ từ số điện thoại {}", maskPhone(phone));
        } catch (RuntimeException ex) {
            throw new ServiceUnavailableException("Không thể gửi thông tin liên hệ lúc này. Vui lòng thử lại sau", ex);
        }
    }

    private void incrementRateLimit(String key) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, RATE_LIMIT_MINUTES, TimeUnit.MINUTES);
            }
            if (count != null && count > MAX_SENDS) {
                throw new BadRequestException("Bạn đã gửi quá nhiều lần. Vui lòng thử lại sau 10 phút");
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            log.warn("Redis không khả dụng khi kiểm tra giới hạn gửi liên hệ");
            throw new ServiceUnavailableException("Kênh liên hệ tạm thời chưa sẵn sàng. Vui lòng thử lại sau", ex);
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateNormalized(String fullName, String subject, String message) {
        if (fullName.length() < 2) {
            throw new BadRequestException("Họ tên phải có từ 2 đến 100 ký tự");
        }
        if (subject.length() < 3) {
            throw new BadRequestException("Chủ đề phải có từ 3 đến 150 ký tự");
        }
        if (message.length() < 10) {
            throw new BadRequestException("Nội dung phải có từ 10 đến 1000 ký tự");
        }
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 4) return "****";
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng", ex);
        }
    }
}
