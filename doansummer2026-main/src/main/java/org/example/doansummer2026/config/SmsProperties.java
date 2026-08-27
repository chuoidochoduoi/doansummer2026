package org.example.doansummer2026.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình SMS gateway (Twilio, Zalo OA, ...).
 */
@ConfigurationProperties(prefix = "app.sms")
public record SmsProperties(
        String provider,
        Twilio twilio,
        Zalo zalo
) {
    public record Twilio(String accountSid, String authToken, String from) {}
    public record Zalo(String appId, String secret, String templateId) {}
}



