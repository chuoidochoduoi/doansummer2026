package org.example.doansummer2026.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "OTP Service");
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực OTP của bạn");

            String htmlContent = String.format(
                "<p>Mã xác thực (OTP) của bạn là: <strong style=\"font-size: 1.2em; color: #0056b3;\">%s</strong></p>" +
                "<p>Mã có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>",
                otpCode
            );

            helper.setText(htmlContent, true);

            mailSender.send(message);
            
            log.info("Da gui OTP {} den email {} qua Gmail SMTP.", otpCode, toEmail);
        } catch (Exception e) {
            log.error("Loi khi gui email OTP den {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Không thể gửi email lúc này. Vui lòng thử lại sau.", e);
        }
    }
}
