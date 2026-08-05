package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Mã xác thực OTP của bạn");
            message.setText("Mã xác thực (OTP) của bạn là: " + otpCode + "\nMã có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.");
            
            javaMailSender.send(message);
            log.info("Da gui OTP {} den email {}", otpCode, toEmail);
        } catch (Exception e) {
            log.error("Loi khi gui email OTP den {}: {}", toEmail, e.getMessage());
            // We shouldn't throw here to avoid failing if email is down, or maybe we should?
            // Usually we throw so the user knows.
            throw new RuntimeException("Không thể gửi email lúc này. Vui lòng thử lại sau.", e);
        }
    }
}
