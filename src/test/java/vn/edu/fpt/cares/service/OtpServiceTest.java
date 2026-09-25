package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SmsService smsService;

    @Mock
    private EmailService emailService;

    private OtpService otpService;


    // =========================================================
    // SETUP
    // =========================================================

    @BeforeEach
    void setUp() {

        otpService = new OtpService(
                redisTemplate,
                smsService,
                emailService
        );

        ReflectionTestUtils.setField(
                otpService,
                "cooldownSeconds",
                60L
        );

        ReflectionTestUtils.setField(
                otpService,
                "maxSendsPerHour",
                5L
        );

        ReflectionTestUtils.setField(
                otpService,
                "maxVerifyAttempts",
                5L
        );

        lenient()
                .when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);
    }


    // =========================================================
    // SEND OTP - PHONE
    // =========================================================

    @Test
    void sendOtp_ShouldSendSms_WhenIdentifierIsPhone() {

        String phone = "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp:cooldown:" + phone
                )
        ).thenReturn(false);

        when(
                valueOperations.increment(
                        "otp:send-count:" + phone
                )
        ).thenReturn(1L);

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(null);

        String code =
                otpService.sendOtp(phone);

        assertNotNull(code);

        assertEquals(
                6,
                code.length()
        );

        assertTrue(
                code.matches("\\d{6}")
        );

        verify(redisTemplate)
                .expire(
                        "otp:send-count:" + phone,
                        1,
                        TimeUnit.HOURS
                );

        verify(valueOperations)
                .set(
                        "otp:" + phone,
                        code,
                        5L,
                        TimeUnit.MINUTES
                );

        verify(valueOperations)
                .set(
                        "otp:cooldown:" + phone,
                        "1",
                        60L,
                        TimeUnit.SECONDS
                );

        verify(redisTemplate)
                .delete(
                        "otp:verify-count:" + phone
                );

        verify(smsService)
                .sendOtp(
                        phone,
                        code
                );

        verifyNoInteractions(
                emailService
        );
    }

    @Test
    void sendOtp_RegeneratesWhenRandomCodeMatchesPreviousCode() {
        String phone = "0912345678";
        SecureRandom random = mock(SecureRandom.class);
        ReflectionTestUtils.setField(otpService, "random", random);
        when(redisTemplate.hasKey("otp:cooldown:" + phone)).thenReturn(false);
        when(valueOperations.increment("otp:send-count:" + phone)).thenReturn(1L);
        when(valueOperations.get("otp:" + phone)).thenReturn("123456");
        when(random.nextInt(900_000)).thenReturn(23_456, 554_321);

        String result = otpService.sendOtp(phone);

        assertEquals("654321", result);
        verify(random, times(2)).nextInt(900_000);
        verify(valueOperations).set("otp:" + phone, "654321", 5L, TimeUnit.MINUTES);
    }


    // =========================================================
    // SEND OTP - EMAIL
    // =========================================================

    @Test
    void sendOtp_ShouldSendEmail_WhenIdentifierIsEmail() {

        String email =
                "test@example.com";

        when(
                redisTemplate.hasKey(
                        "otp:cooldown:" + email
                )
        ).thenReturn(false);

        when(
                valueOperations.increment(
                        "otp:send-count:" + email
                )
        ).thenReturn(1L);

        when(
                valueOperations.get(
                        "otp:" + email
                )
        ).thenReturn(null);

        String code =
                otpService.sendOtp(email);

        assertNotNull(code);

        assertEquals(
                6,
                code.length()
        );

        verify(emailService)
                .sendOtpEmail(
                        email,
                        code
                );

        verifyNoInteractions(
                smsService
        );
    }


    // =========================================================
    // SEND OTP - NORMALIZATION
    // =========================================================

    @Test
    void sendOtp_ShouldNormalizeIdentifier() {

        String input =
                "  TEST@EXAMPLE.COM  ";

        String normalized =
                "test@example.com";

        when(
                redisTemplate.hasKey(
                        "otp:cooldown:" + normalized
                )
        ).thenReturn(false);

        when(
                valueOperations.increment(
                        "otp:send-count:" + normalized
                )
        ).thenReturn(1L);

        when(
                valueOperations.get(
                        "otp:" + normalized
                )
        ).thenReturn(null);

        String code =
                otpService.sendOtp(input);

        verify(emailService)
                .sendOtpEmail(
                        normalized,
                        code
                );

        verify(valueOperations)
                .set(
                        "otp:" + normalized,
                        code,
                        5L,
                        TimeUnit.MINUTES
                );
    }


    // =========================================================
    // SEND OTP - COOLDOWN
    // =========================================================

    @Test
    void sendOtp_ShouldThrow_WhenCooldownExists() {

        String phone =
                "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp:cooldown:" + phone
                )
        ).thenReturn(true);

        when(
                redisTemplate.getExpire(
                        "otp:cooldown:" + phone,
                        TimeUnit.SECONDS
                )
        ).thenReturn(35L);

        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                otpService.sendOtp(
                                        phone
                                )
                );

        assertEquals(
                "Vui lòng chờ 35 giây trước khi gửi lại OTP",
                ex.getMessage()
        );

        verify(
                valueOperations,
                never()
        ).increment(
                anyString()
        );

        verifyNoInteractions(
                smsService
        );

        verifyNoInteractions(
                emailService
        );
    }


    @Test
    void sendOtp_ShouldUseOneSecond_WhenCooldownTtlIsNull() {

        String phone =
                "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp:cooldown:" + phone
                )
        ).thenReturn(true);

        when(
                redisTemplate.getExpire(
                        "otp:cooldown:" + phone,
                        TimeUnit.SECONDS
                )
        ).thenReturn(null);

        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                otpService.sendOtp(
                                        phone
                                )
                );

        assertEquals(
                "Vui lòng chờ 1 giây trước khi gửi lại OTP",
                ex.getMessage()
        );
    }


    @Test
    void sendOtp_ShouldUseOneSecond_WhenCooldownTtlIsNegative() {

        String phone =
                "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp:cooldown:" + phone
                )
        ).thenReturn(true);

        when(
                redisTemplate.getExpire(
                        "otp:cooldown:" + phone,
                        TimeUnit.SECONDS
                )
        ).thenReturn(-1L);

        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                otpService.sendOtp(
                                        phone
                                )
                );

        assertEquals(
                "Vui lòng chờ 1 giây trước khi gửi lại OTP",
                ex.getMessage()
        );
    }


    // =========================================================
    // SEND OTP - SEND COUNT
    // =========================================================

    @Test
    void sendOtp_ShouldNotExpireSendCountAgain_WhenCountGreaterThanOne() {

        String phone =
                "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp:cooldown:" + phone
                )
        ).thenReturn(false);

        when(
                valueOperations.increment(
                        "otp:send-count:" + phone
                )
        ).thenReturn(2L);

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(null);

        otpService.sendOtp(phone);

        verify(
                redisTemplate,
                never()
        ).expire(
                "otp:send-count:" + phone,
                1,
                TimeUnit.HOURS
        );

        verify(smsService)
                .sendOtp(
                        eq(phone),
                        anyString()
                );
    }


    @Test
    void sendOtp_ShouldThrow_WhenSendLimitExceeded() {

        String phone =
                "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp:cooldown:" + phone
                )
        ).thenReturn(false);

        when(
                valueOperations.increment(
                        "otp:send-count:" + phone
                )
        ).thenReturn(6L);

        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                otpService.sendOtp(
                                        phone
                                )
                );

        assertEquals(
                "Đã vượt quá số lần gửi OTP cho phép trong một giờ",
                ex.getMessage()
        );

        verifyNoInteractions(
                smsService
        );

        verifyNoInteractions(
                emailService
        );

        verify(
                valueOperations,
                never()
        ).set(
                startsWith("otp:"),
                anyString(),
                anyLong(),
                any(TimeUnit.class)
        );
    }


    @Test
    void sendOtp_ShouldAllowExactlyMaximumSendCount() {

        String phone =
                "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp:cooldown:" + phone
                )
        ).thenReturn(false);

        when(
                valueOperations.increment(
                        "otp:send-count:" + phone
                )
        ).thenReturn(5L);

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(null);

        String code =
                otpService.sendOtp(phone);

        assertNotNull(code);

        verify(smsService)
                .sendOtp(
                        phone,
                        code
                );
    }


    @Test
    void sendOtp_ShouldStillWork_WhenIncrementReturnsNull() {

        String phone =
                "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp:cooldown:" + phone
                )
        ).thenReturn(false);

        when(
                valueOperations.increment(
                        "otp:send-count:" + phone
                )
        ).thenReturn(null);

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(null);

        String code =
                otpService.sendOtp(phone);

        assertNotNull(code);

        verify(smsService)
                .sendOtp(
                        phone,
                        code
                );
    }


    @Test
    void sendOtp_ShouldDeletePreviousVerifyCount() {

        String phone =
                "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp:cooldown:" + phone
                )
        ).thenReturn(false);

        when(
                valueOperations.increment(
                        "otp:send-count:" + phone
                )
        ).thenReturn(1L);

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(null);

        otpService.sendOtp(phone);

        verify(redisTemplate)
                .delete(
                        "otp:verify-count:" + phone
                );
    }


    @Test
    void sendOtp_ShouldStoreOtpWithFiveMinuteTtl() {

        String phone =
                "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp:cooldown:" + phone
                )
        ).thenReturn(false);

        when(
                valueOperations.increment(
                        "otp:send-count:" + phone
                )
        ).thenReturn(1L);

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(null);

        String code =
                otpService.sendOtp(phone);

        verify(valueOperations)
                .set(
                        "otp:" + phone,
                        code,
                        5L,
                        TimeUnit.MINUTES
                );
    }


    // =========================================================
    // VERIFY OTP - SUCCESS
    // =========================================================

    @Test
    void verifyOtp_ShouldReturnTrue_WhenOtpCorrect() {

        String phone =
                "0912345678";

        String code =
                "123456";

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(code);

        boolean result =
                otpService.verifyOtp(
                        phone,
                        code
                );

        assertTrue(result);

        verify(redisTemplate)
                .delete(
                        "otp:" + phone
                );

        verify(redisTemplate)
                .delete(
                        "otp:verify-count:" + phone
                );
    }


    // =========================================================
    // VERIFY OTP - NOT FOUND
    // =========================================================

    @Test
    void verifyOtp_ShouldReturnFalse_WhenOtpDoesNotExist() {

        String phone =
                "0912345678";

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(null);

        boolean result =
                otpService.verifyOtp(
                        phone,
                        "123456"
                );

        assertFalse(result);

        verify(
                redisTemplate,
                never()
        ).delete(
                "otp:" + phone
        );
    }


    // =========================================================
    // VERIFY OTP - WRONG CODE
    // =========================================================

    @Test
    void verifyOtp_ShouldThrow_WhenOtpIncorrect() {

        String phone =
                "0912345678";

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(
                "123456"
        );

        when(
                valueOperations.increment(
                        "otp:verify-count:" + phone
                )
        ).thenReturn(2L);

        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                otpService.verifyOtp(
                                        phone,
                                        "999999"
                                )
                );

        assertEquals(
                "Mã OTP không chính xác",
                ex.getMessage()
        );

        verify(
                redisTemplate,
                never()
        ).delete(
                "otp:" + phone
        );
    }


    @Test
    void verifyOtp_ShouldSetVerifyCounterExpiry_OnFirstFailure() {

        String phone =
                "0912345678";

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(
                "123456"
        );

        when(
                valueOperations.increment(
                        "otp:verify-count:" + phone
                )
        ).thenReturn(1L);

        assertThrows(
                BadRequestException.class,
                () ->
                        otpService.verifyOtp(
                                phone,
                                "999999"
                        )
        );

        verify(redisTemplate)
                .expire(
                        "otp:verify-count:" + phone,
                        5L,
                        TimeUnit.MINUTES
                );
    }


    @Test
    void verifyOtp_ShouldNotResetCounterExpiry_AfterFirstFailure() {

        String phone =
                "0912345678";

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(
                "123456"
        );

        when(
                valueOperations.increment(
                        "otp:verify-count:" + phone
                )
        ).thenReturn(3L);

        assertThrows(
                BadRequestException.class,
                () ->
                        otpService.verifyOtp(
                                phone,
                                "999999"
                        )
        );

        verify(
                redisTemplate,
                never()
        ).expire(
                "otp:verify-count:" + phone,
                5L,
                TimeUnit.MINUTES
        );
    }


    // =========================================================
    // VERIFY OTP - MAX ATTEMPTS
    // =========================================================

    @Test
    void verifyOtp_ShouldDeleteOtpAndThrow_WhenMaximumAttemptsReached() {

        String phone =
                "0912345678";

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(
                "123456"
        );

        when(
                valueOperations.increment(
                        "otp:verify-count:" + phone
                )
        ).thenReturn(5L);

        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                otpService.verifyOtp(
                                        phone,
                                        "999999"
                                )
                );

        assertEquals(
                "Đã nhập sai OTP quá số lần cho phép; vui lòng gửi mã mới",
                ex.getMessage()
        );

        verify(redisTemplate)
                .delete(
                        "otp:" + phone
                );
    }


    @Test
    void verifyOtp_ShouldDeleteOtp_WhenAttemptsGreaterThanMaximum() {

        String phone =
                "0912345678";

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(
                "123456"
        );

        when(
                valueOperations.increment(
                        "otp:verify-count:" + phone
                )
        ).thenReturn(6L);

        assertThrows(
                BadRequestException.class,
                () ->
                        otpService.verifyOtp(
                                phone,
                                "999999"
                        )
        );

        verify(redisTemplate)
                .delete(
                        "otp:" + phone
                );
    }


    @Test
    void verifyOtp_ShouldHandleNullAttemptCounter() {

        String phone =
                "0912345678";

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(
                "123456"
        );

        when(
                valueOperations.increment(
                        "otp:verify-count:" + phone
                )
        ).thenReturn(null);

        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                otpService.verifyOtp(
                                        phone,
                                        "999999"
                                )
                );

        assertEquals(
                "Mã OTP không chính xác",
                ex.getMessage()
        );

        verify(
                redisTemplate,
                never()
        ).delete(
                "otp:" + phone
        );
    }


    // =========================================================
    // VERIFY OTP - NORMALIZATION
    // =========================================================

    @Test
    void verifyOtp_ShouldNormalizeEmail() {

        String input =
                "  TEST@EXAMPLE.COM ";

        String normalized =
                "test@example.com";

        when(
                valueOperations.get(
                        "otp:" + normalized
                )
        ).thenReturn(
                "123456"
        );

        boolean result =
                otpService.verifyOtp(
                        input,
                        "123456"
                );

        assertTrue(result);

        verify(redisTemplate)
                .delete(
                        "otp:" + normalized
                );

        verify(redisTemplate)
                .delete(
                        "otp:verify-count:" + normalized
                );
    }


    @Test
    void verifyOtp_ShouldNormalizePhoneWhitespace() {

        String input =
                "  0912345678  ";

        String normalized =
                "0912345678";

        when(
                valueOperations.get(
                        "otp:" + normalized
                )
        ).thenReturn(
                "123456"
        );

        assertTrue(
                otpService.verifyOtp(
                        input,
                        "123456"
                )
        );

        verify(valueOperations)
                .get(
                        "otp:" + normalized
                );
    }


    @Test
    void verifyOtp_ShouldReturnFalse_WhenIdentifierNull() {

        when(
                valueOperations.get(
                        "otp:"
                )
        ).thenReturn(null);

        boolean result =
                otpService.verifyOtp(
                        null,
                        "123456"
                );

        assertFalse(result);

        verify(valueOperations)
                .get(
                        "otp:"
                );
    }


    @Test
    void verifyOtp_ShouldReturnFalse_WhenIdentifierBlankAndNoOtp() {

        when(
                valueOperations.get(
                        "otp:"
                )
        ).thenReturn(null);

        boolean result =
                otpService.verifyOtp(
                        "   ",
                        "123456"
                );

        assertFalse(result);
    }


    // =========================================================
    // VERIFIED OTP
    // =========================================================

    @Test
    void markOtpAsVerified_ShouldStoreVerifiedMarker_WhenOtpValid() {

        String phone =
                "0912345678";

        String code =
                "123456";

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(code);

        otpService.markOtpAsVerified(
                phone,
                code
        );

        verify(redisTemplate)
                .delete(
                        "otp:" + phone
                );

        verify(redisTemplate)
                .delete(
                        "otp:verify-count:" + phone
                );

        verify(valueOperations)
                .set(
                        "otp_verified:" + phone,
                        "true",
                        15L,
                        TimeUnit.MINUTES
                );
    }


    @Test
    void markOtpAsVerified_ShouldThrow_WhenOtpMissing() {

        String phone =
                "0912345678";

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(null);

        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                otpService.markOtpAsVerified(
                                        phone,
                                        "123456"
                                )
                );

        assertEquals(
                "OTP không hợp lệ hoặc đã hết hạn",
                ex.getMessage()
        );

        verify(
                valueOperations,
                never()
        ).set(
                startsWith("otp_verified:"),
                anyString(),
                anyLong(),
                any(TimeUnit.class)
        );
    }


    @Test
    void isOtpVerified_ShouldReturnTrue_WhenVerifiedMarkerExists() {

        String phone =
                "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp_verified:" + phone
                )
        ).thenReturn(true);

        assertTrue(
                otpService.isOtpVerified(
                        phone
                )
        );
    }


    @Test
    void isOtpVerified_ShouldReturnFalse_WhenVerifiedMarkerMissing() {

        String phone =
                "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp_verified:" + phone
                )
        ).thenReturn(false);

        assertFalse(
                otpService.isOtpVerified(
                        phone
                )
        );
    }


    @Test
    void isOtpVerified_ShouldReturnFalse_WhenRedisReturnsNull() {

        String phone =
                "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp_verified:" + phone
                )
        ).thenReturn(null);

        assertFalse(
                otpService.isOtpVerified(
                        phone
                )
        );
    }


    @Test
    void consumeVerifiedOtp_ShouldDeleteVerifiedMarker() {

        String input =
                "  TEST@EXAMPLE.COM ";

        otpService.consumeVerifiedOtp(
                input
        );

        verify(redisTemplate)
                .delete(
                        "otp_verified:test@example.com"
                );
    }


    // =========================================================
    // CONFIGURATION BOUNDARIES
    // =========================================================

    @Test
    void sendOtp_ShouldRespectCustomCooldownSeconds() {

        ReflectionTestUtils.setField(
                otpService,
                "cooldownSeconds",
                120L
        );

        String phone =
                "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp:cooldown:" + phone
                )
        ).thenReturn(false);

        when(
                valueOperations.increment(
                        "otp:send-count:" + phone
                )
        ).thenReturn(1L);

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(null);

        otpService.sendOtp(phone);

        verify(valueOperations)
                .set(
                        "otp:cooldown:" + phone,
                        "1",
                        120L,
                        TimeUnit.SECONDS
                );
    }


    @Test
    void sendOtp_ShouldRespectCustomMaxSendsPerHour() {

        ReflectionTestUtils.setField(
                otpService,
                "maxSendsPerHour",
                2L
        );

        String phone =
                "0912345678";

        when(
                redisTemplate.hasKey(
                        "otp:cooldown:" + phone
                )
        ).thenReturn(false);

        when(
                valueOperations.increment(
                        "otp:send-count:" + phone
                )
        ).thenReturn(3L);

        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                otpService.sendOtp(
                                        phone
                                )
                );

        assertEquals(
                "Đã vượt quá số lần gửi OTP cho phép trong một giờ",
                ex.getMessage()
        );
    }


    @Test
    void verifyOtp_ShouldRespectCustomMaximumAttempts() {

        ReflectionTestUtils.setField(
                otpService,
                "maxVerifyAttempts",
                3L
        );

        String phone =
                "0912345678";

        when(
                valueOperations.get(
                        "otp:" + phone
                )
        ).thenReturn(
                "123456"
        );

        when(
                valueOperations.increment(
                        "otp:verify-count:" + phone
                )
        ).thenReturn(3L);

        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                otpService.verifyOtp(
                                        phone,
                                        "000000"
                                )
                );

        assertEquals(
                "Đã nhập sai OTP quá số lần cho phép; vui lòng gửi mã mới",
                ex.getMessage()
        );

        verify(redisTemplate)
                .delete(
                        "otp:" + phone
                );
    }
}
