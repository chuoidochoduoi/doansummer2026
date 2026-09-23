package org.example.doansummer2026.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BrevoEmailClient {

    private final RestClient restClient;

    public BrevoEmailClient(
            @Value("${app.brevo.api-url:https://api.brevo.com/v3/smtp/email}") String apiUrl) {
        this.restClient = RestClient.builder().baseUrl(apiUrl).build();
    }

    public void send(String apiKey, String senderEmail, String senderName,
                     String recipientEmail, String subject, String htmlContent,
                     String replyToEmail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sender", Map.of("email", senderEmail, "name", senderName));
        body.put("to", List.of(Map.of("email", recipientEmail)));
        body.put("subject", subject);
        body.put("htmlContent", htmlContent);
        if (replyToEmail != null && !replyToEmail.isBlank()) {
            body.put("replyTo", Map.of("email", replyToEmail));
        }

        restClient.post()
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
