package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.config.SmsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Gửi SMS qua Zalo OA ZNS API.
 * Bật bằng cách: app.sms.provider=zalo
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "zalo")
public class ZaloSmsService implements SmsService {

    private final SmsProperties smsProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendOtp(String phone, String code) {
        String url = "https://openapi.zalo.me/v3.0/oa/message";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "recipient", Map.of("phone", phone),
                "template_id", smsProperties.zalo().templateId(),
                "template_data", Map.of("code", code)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForObject(url, request, String.class);
    }
}