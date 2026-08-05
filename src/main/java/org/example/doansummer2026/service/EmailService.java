package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    @Value("${app.brevo.api-key:}")
    private String brevoApiKey;

    @Value("${app.brevo.from-email:chuoidochoduoi7e@gmail.com}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otpCode) {
        if (brevoApiKey == null || brevoApiKey.trim().isEmpty()) {
            System.out.println("[MOCK EMAIL] Gửi OTP " + otpCode + " đến " + toEmail);
            log.info("Mocked email OTP since Brevo API key is empty");
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            headers.set("accept", "application/json");

            String body = String.format(
                "{\"sender\": {\"email\": \"%s\", \"name\": \"OTP Service\"}, \"to\": [{\"email\": \"%s\"}], \"subject\": \"Mã xác thực OTP của bạn\", \"htmlContent\": \"<p>Mã xác thực (OTP) của bạn là: <strong>%s</strong></p><p>Mã có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>\"}",
                fromEmail, toEmail, otpCode
            );

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject("https://api.brevo.com/v3/smtp/email", request, String.class);
            
            log.info("Da gui OTP {} den email {} qua Brevo. Response: {}", otpCode, toEmail, response);
        } catch (Exception e) {
            log.error("Loi khi gui email OTP den {} qua Brevo: {}", toEmail, e.getMessage());
            throw new RuntimeException("Không thể gửi email lúc này. Vui lòng thử lại sau.", e);
        }
    }
}
