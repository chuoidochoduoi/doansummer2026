package org.example.doansummer2026.service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    @Mock JavaMailSender mailSender;
    private EmailService service;

    @BeforeEach
    void setUp() {
        service = new EmailService(mailSender);
        ReflectionTestUtils.setField(service, "fromEmail", "no-reply@cares.vn");
    }

    @Test
    void otpEmailUsesUtf8HtmlAndExpectedHeaders() throws Exception {
        MimeMessage message = message();
        when(mailSender.createMimeMessage()).thenReturn(message);

        service.sendOtpEmail("duc@example.com", "123456");

        verify(mailSender).send(message);
        assertEquals("Mã xác thực OTP của bạn", message.getSubject());
        assertEquals("duc@example.com", ((InternetAddress) message.getAllRecipients()[0]).getAddress());
        assertEquals("no-reply@cares.vn", ((InternetAddress) message.getFrom()[0]).getAddress());
        String body = body(message);
        assertTrue(body.contains("123456"));
        assertTrue(body.contains("5 phút"));
    }

    @Test
    void otpFailureReturnsStableMessageAndKeepsCause() {
        IllegalStateException cause = new IllegalStateException("SMTP offline");
        when(mailSender.createMimeMessage()).thenThrow(cause);
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.sendOtpEmail("duc@example.com", "123456"));
        assertEquals("Không thể gửi email lúc này. Vui lòng thử lại sau.", error.getMessage());
        assertSame(cause, error.getCause());
    }

    @Test
    void contactEmailSanitizesSubjectEscapesHtmlAndSetsReplyTo() throws Exception {
        MimeMessage message = message();
        when(mailSender.createMimeMessage()).thenReturn(message);

        service.sendContactEmail("contact@cares.vn", "Nguyễn <Đức>", "0987&654321",
                "duc@example.com", "Hỗ trợ\r\nkhẩn", "Dòng <một>\nDòng & hai");

        verify(mailSender).send(message);
        assertEquals("[Liên hệ CareS] Hỗ trợ khẩn", message.getSubject());
        assertEquals("duc@example.com", ((InternetAddress) message.getReplyTo()[0]).getAddress());
        String body = body(message);
        assertTrue(body.contains("Nguyễn &lt;Đức&gt;"));
        assertTrue(body.contains("0987&amp;654321"));
        assertTrue(body.contains("&lt;một&gt;<br>"), body);
        assertTrue(body.contains("&amp; hai"), body);
    }

    @Test
    void contactEmailWithoutCustomerEmailDoesNotSetReplyToAndShowsFallback() throws Exception {
        MimeMessage message = message();
        when(mailSender.createMimeMessage()).thenReturn(message);
        service.sendContactEmail("contact@cares.vn", "Nguyễn Anh Đức", "0987654321",
                " ", "Hỗ trợ", "Nội dung yêu cầu");
        assertNull(message.getHeader("Reply-To"));
        String blankBody = body(message);
        assertTrue(blankBody.contains("Email:</strong>"), blankBody);
        assertFalse(blankBody.contains("mailto:"), blankBody);

        MimeMessage nullEmail = message();
        when(mailSender.createMimeMessage()).thenReturn(nullEmail);
        service.sendContactEmail("contact@cares.vn", "Nguyễn Anh Đức", "0987654321",
                null, "Hỗ trợ", "Nội dung yêu cầu");
        assertNull(nullEmail.getHeader("Reply-To"));
        assertTrue(body(nullEmail).contains("Email:</strong>"));
    }

    @Test
    void contactDeliveryFailureReturnsStableErrorAndCause() {
        MimeMessage message = message();
        when(mailSender.createMimeMessage()).thenReturn(message);
        IllegalStateException cause = new IllegalStateException("provider secret");
        doThrow(cause).when(mailSender).send(message);
        RuntimeException error = assertThrows(RuntimeException.class, () -> service.sendContactEmail(
                "contact@cares.vn", "Nguyễn Anh Đức", "0987654321", null, "Hỗ trợ", "Nội dung yêu cầu"));
        assertEquals("Không thể gửi thông tin liên hệ qua email", error.getMessage());
        assertSame(cause, error.getCause());
    }

    private MimeMessage message() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private String body(Part part) throws Exception {
        if (part instanceof MimeMessage message) message.saveChanges();
        Object content = part.getContent();
        if (content instanceof String text) return text;
        if (content instanceof Multipart multipart) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) result.append(body(multipart.getBodyPart(i)));
            return result.toString();
        }
        return String.valueOf(content);
    }
}
