package org.example.doansummer2026.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.config.SmsProperties;
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

    @Override
    public void sendOtp(String phone, String code) {
        String body = "Ma xac thuc dang ky tai khoan cua ban la: " + code + ". Ma co hieu luc 5 phut.";

        Message.creator(
                new PhoneNumber(phone),
                new PhoneNumber(smsProperties.twilio().from()),
                body
        ).create();
    }
}