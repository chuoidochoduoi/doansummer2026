package vn.edu.fpt.cares.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.config.SmsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Gửi SMS qua Twilio (mặc định khi không cấu hình gateway khác).
 * Bật bằng cách: app.sms.provider=twilio
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "twilio", matchIfMissing = true)
public class TwilioSmsService implements SmsService {

    private final SmsProperties smsProperties;

    @PostConstruct
    void initializeTwilio() {
        SmsProperties.Twilio config = smsProperties.twilio();
        if (config == null || isBlank(config.accountSid())
                || isBlank(config.authToken()) || isBlank(config.from())) {
            throw new IllegalStateException(
                    "Twilio chưa được cấu hình đầy đủ: cần TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN và TWILIO_FROM");
        }
        Twilio.init(config.accountSid().trim(), config.authToken().trim());
    }

    @Override
    public void sendOtp(String phone, String code) {
        String body = "Ma xac thuc dang ky tai khoan cua ban la: " + code + ". Ma co hieu luc 5 phut.";

        Message.creator(
                new PhoneNumber(normalizeVietnamesePhone(phone)),
                new PhoneNumber(smsProperties.twilio().from().trim()),
                body
        ).create();
    }

    private String normalizeVietnamesePhone(String phone) {
        if (isBlank(phone)) {
            throw new IllegalArgumentException("Số điện thoại nhận OTP không được để trống");
        }

        String normalized = phone.trim().replaceAll("[\\s.-]", "");
        if (normalized.startsWith("+84")) {
            return normalized;
        }
        if (normalized.startsWith("84")) {
            return "+" + normalized;
        }
        if (normalized.startsWith("0")) {
            return "+84" + normalized.substring(1);
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}



