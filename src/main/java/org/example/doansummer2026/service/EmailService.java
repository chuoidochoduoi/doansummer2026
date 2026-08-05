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

    @Value("${app.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otpCode) {
        if (resendApiKey == null || resendApiKey.trim().isEmpty()) {
            System.out.println("[MOCK EMAIL] Gửi OTP " + otpCode + " đến " + toEmail);
            log.info("Mocked email OTP since Resend API key is empty");
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            String body = String.format(
                "{\"from\": \"%s\", \"to\": [\"%s\"], \"subject\": \"Mã xác thực OTP của bạn\", \"html\": \"<p>Mã xác thực (OTP) của bạn là: <strong>%s</strong></p><p>Mã có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>\"}",
                fromEmail, toEmail, otpCode
            );

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject("https://api.resend.com/emails", request, String.class);
            
            log.info("Da gui OTP {} den email {} qua Resend. Response: {}", otpCode, toEmail, response);
        } catch (Exception e) {
            log.error("Loi khi gui email OTP den {} qua Resend: {}", toEmail, e.getMessage());
            throw new RuntimeException("Không thể gửi email lúc này. Vui lòng thử lại sau.", e);
        }
    }
}
