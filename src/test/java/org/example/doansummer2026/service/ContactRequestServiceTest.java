package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.contact.ContactRequestCreateRequest;
import org.example.doansummer2026.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactRequestServiceTest {
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> values;
    @Mock EmailService emailService;
    @InjectMocks ContactRequestService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(values);
        configure("contact@cares.vn", "mailer@cares.vn", "secret");
    }

    @Test
    void rejectsEveryMissingMailConfigurationBeforeReadingRequest() {
        configure(" ", "mailer", "secret");
        assertThrows(ServiceUnavailableException.class, () -> service.send(null));
        configure("contact", null, "secret");
        assertThrows(ServiceUnavailableException.class, () -> service.send(null));
        configure("contact", "mailer", "");
        assertThrows(ServiceUnavailableException.class, () -> service.send(null));
        verifyNoInteractions(emailService);
    }

    @Test
    void sendsTrimmedContactDataAndStartsRateLimitWindow() {
        when(values.increment(anyString())).thenReturn(1L);
        service.send(request(" Nguyễn Anh Đức ", " 0987654321 ", " duc@example.com ",
                " Hỗ trợ đặt lịch ", " Tôi cần hỗ trợ đặt lịch khám "));

        verify(emailService).sendContactEmail("contact@cares.vn", "Nguyễn Anh Đức", "0987654321",
                "duc@example.com", "Hỗ trợ đặt lịch", "Tôi cần hỗ trợ đặt lịch khám");
        verify(redisTemplate).expire(startsWith("contact:send-count:"), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void optionalEmailAndShortPhoneAreHandledWithoutChangingDelivery() {
        when(values.increment(anyString())).thenReturn(null, 2L);
        service.send(request("An", "1234", " ", "Hỏi", "Nội dung đủ dài"));
        service.send(request("An", "12345", null, "Hỏi", "Nội dung đủ dài"));
        verify(emailService, times(2)).sendContactEmail(eq("contact@cares.vn"), eq("An"), anyString(),
                isNull(), eq("Hỏi"), eq("Nội dung đủ dài"));
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void normalizedLengthValidationRejectsEachInvalidField() {
        assertThrows(BadRequestException.class,
                () -> service.send(request("A", "0987654321", null, "Hỏi", "Nội dung đủ dài")));
        assertThrows(BadRequestException.class,
                () -> service.send(request("An", "0987654321", null, "Hi", "Nội dung đủ dài")));
        assertThrows(BadRequestException.class,
                () -> service.send(request("An", "0987654321", null, "Hỏi", "quá ngắn")));
        verifyNoInteractions(emailService);
    }

    @Test
    void fourthRequestIsRateLimitedAndRedisFailureBecomesServiceUnavailable() {
        when(values.increment(anyString())).thenReturn(4L);
        assertThrows(BadRequestException.class, () -> service.send(validRequest()));
        verifyNoInteractions(emailService);

        when(values.increment(anyString())).thenThrow(new RedisConnectionFailureException("offline"));
        assertThrows(ServiceUnavailableException.class, () -> service.send(validRequest()));
        verifyNoInteractions(emailService);
    }

    @Test
    void emailFailureIsHiddenBehindStableServiceUnavailableError() {
        when(values.increment(anyString())).thenReturn(2L);
        doThrow(new IllegalStateException("SMTP password leaked"))
                .when(emailService).sendContactEmail(anyString(), anyString(), anyString(), any(), anyString(), anyString());
        ServiceUnavailableException error = assertThrows(ServiceUnavailableException.class,
                () -> service.send(validRequest()));
        assertFalse(error.getMessage().contains("password"));
        assertTrue(error.getMessage().contains("Vui lòng thử lại sau"));
    }

    @Test
    void unavailableSha256ProviderIsReportedAsIllegalState() throws Exception {
        try (var digest = mockStatic(MessageDigest.class)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException("disabled for test"));

            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> service.send(validRequest()));

            assertTrue(error.getMessage().contains("SHA-256"));
            verifyNoInteractions(emailService);
        }
    }

    private void configure(String recipient, String username, String password) {
        ReflectionTestUtils.setField(service, "recipientEmail", recipient);
        ReflectionTestUtils.setField(service, "mailUsername", username);
        ReflectionTestUtils.setField(service, "mailPassword", password);
    }

    private ContactRequestCreateRequest validRequest() {
        return request("Nguyễn Anh Đức", "0987654321", "duc@example.com",
                "Hỗ trợ đặt lịch", "Tôi cần hỗ trợ đặt lịch khám");
    }

    private ContactRequestCreateRequest request(String name, String phone, String email,
                                                String subject, String message) {
        return new ContactRequestCreateRequest(name, phone, email, subject, message);
    }
}
