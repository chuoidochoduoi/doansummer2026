package org.example.doansummer2026.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final BrevoEmailClient brevoEmailClient;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.brevo.api-key:}")
    private String brevoApiKey;

    @Value("${app.brevo.sender-email:${spring.mail.username:}}")
    private String brevoSenderEmail;

    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            String htmlContent = String.format(
                "<p>Mã xác thực (OTP) của bạn là: <strong style=\"font-size: 1.2em; color: #0056b3;\">%s</strong></p>" +
                "<p>Mã có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>",
                otpCode
            );
            sendHtml(toEmail, "Mã xác thực OTP của bạn", htmlContent, null, "Phòng khám CareS");
            log.info("Đã gửi email OTP tới địa chỉ được yêu cầu");
        } catch (Exception e) {
            log.error("Loi khi gui email OTP den {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Không thể gửi email lúc này. Vui lòng thử lại sau.", e);
        }
    }

    public void sendContactEmail(String toEmail, String fullName, String phone,
                                 String customerEmail, String subject, String content) {
        try {
            String sentAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
            String htmlContent = "<h3>Thông tin liên hệ mới từ website CareS</h3>"
                    + "<p><strong>Họ tên:</strong> " + HtmlUtils.htmlEscape(fullName) + "</p>"
                    + "<p><strong>Số điện thoại:</strong> " + HtmlUtils.htmlEscape(phone) + "</p>"
                    + "<p><strong>Email:</strong> "
                    + HtmlUtils.htmlEscape(customerEmail == null || customerEmail.isBlank() ? "Không cung cấp" : customerEmail)
                    + "</p><p><strong>Chủ đề:</strong> " + HtmlUtils.htmlEscape(subject) + "</p>"
                    + "<p><strong>Thời gian gửi:</strong> " + sentAt + "</p>"
                    + "<p><strong>Nội dung:</strong><br>"
                    + HtmlUtils.htmlEscape(content).replace("\n", "<br>") + "</p>";
            sendHtml(toEmail, "[Liên hệ CareS] " + sanitizeSubject(subject), htmlContent,
                    customerEmail, "CareS Clinic");
            log.info("Đã gửi thông tin liên hệ tới hộp thư tiếp nhận");
        } catch (Exception e) {
            log.warn("Không thể gửi thông tin liên hệ tới hộp thư tiếp nhận: {}", e.getClass().getSimpleName());
            throw new RuntimeException("Không thể gửi thông tin liên hệ qua email", e);
        }
    }

    private String sanitizeSubject(String subject) {
        return subject.replaceAll("[\\r\\n]+", " ").trim();
    }

    private void sendHtml(String toEmail, String subject, String htmlContent,
                          String replyToEmail, String senderName) throws Exception {
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            String senderEmail = brevoSenderEmail == null ? "" : brevoSenderEmail.trim();
            if (senderEmail.isBlank()) {
                throw new IllegalStateException("BREVO_SENDER_EMAIL hoặc MAIL_USERNAME chưa được cấu hình");
            }
            brevoEmailClient.send(brevoApiKey.trim(), senderEmail, senderName,
                    toEmail, subject, htmlContent, replyToEmail);
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail, senderName);
        helper.setTo(toEmail);
        if (replyToEmail != null && !replyToEmail.isBlank()) {
            helper.setReplyTo(replyToEmail);
        }
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }
}
