package org.example.doansummer2026.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.example.doansummer2026.config.JwtService;
import org.example.doansummer2026.dto.auth.*;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AccountService accounts;
    @Mock ProfileRepository profiles;
    @Mock JwtService jwt;
    @Mock OtpService otp;
    @Mock PasswordEncoder passwords;
    @Mock StaffInfoRepository staff;
    @Mock AppointmentRepository appointments;
    @Mock CustomerVisitRepository visits;
    @Mock InvoiceRepository invoices;
    @Mock AccountRepository accountRepo;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> values;
    @InjectMocks AuthService service;

    private final Account customer = Account.builder().accountId(UUID.randomUUID()).username("internal-user")
            .role(Role.CUSTOMER).passwordHash("hash").isActive(true).build();
    private final Profile profile = Profile.builder().profileId(UUID.randomUUID()).account(customer).build();

    @BeforeEach void configureLimit() {
        ReflectionTestUtils.setField(service, "maxLoginAttempts", 5L);
        ReflectionTestUtils.setField(service, "loginLockMinutes", 15L);
    }

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    private void authentication(Object principal, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    private void tokens(Account account) {
        when(jwt.generateAccessToken(eq(account), any(), any())).thenReturn("access");
        when(jwt.generateRefreshToken(eq(account), any(), any())).thenReturn("refresh");
        when(jwt.getAccessExpirationMs()).thenReturn(60000L);
    }

    private void loginLookup() {
        when(redis.opsForValue()).thenReturn(values);
        when(profiles.findFirstByEmailIgnoreCase("user@example.test")).thenReturn(Optional.of(profile));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "invalid", "123", "user@"})
    void invalidRegistrationIdentifierDoesNotQueryProfiles(String identifier) {
        assertThrows(BadRequestException.class, () -> service.registrationAvailability(identifier));
        verifyNoInteractions(profiles);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void registrationDistinguishesGuestFromLinkedAccount(boolean linked) {
        profile.setAccount(linked ? customer : null);
        when(profiles.findFirstByEmailIgnoreCase("user@example.test")).thenReturn(Optional.of(profile));
        var result = service.registrationAvailability(" user@example.test ");
        assertTrue(result.get("exists"));
        assertEquals(linked, result.get("registered"));
        assertEquals(!linked, result.get("available"));
        if (linked) assertThrows(BadRequestException.class,
                () -> service.ensureRegistrationIdentifierAvailable("user@example.test"));
        else assertDoesNotThrow(() -> service.ensureRegistrationIdentifierAvailable("user@example.test"));
    }

    @Test void phoneAvailabilityUsesAllVietnameseVariants() {
        var result = service.registrationAvailability("+84900000000");
        assertTrue(result.get("available"));
        assertFalse(result.get("exists"));
        verify(profiles).findFirstByPhoneIn(Set.of("+84900000000", "0900000000", "84900000000"));
    }

    @Test void customerLogsInUsingCurrentContactAndReceivesTokens() {
        loginLookup();
        when(passwords.matches("secret", "hash")).thenReturn(true);
        tokens(customer);
        var result = service.login(new LoginRequest(" user@example.test ", "secret"));
        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(60, result.expiresIn());
        assertEquals(customer.getAccountId(), result.account().accountId());
        assertNull(result.account().systemRole());
        verify(redis).delete("auth:login-attempt:user@example.test");
        verifyNoInteractions(accountRepo);
    }

    @Test void staffUsernameRetainsRoleInTokens() {
        when(redis.opsForValue()).thenReturn(values);
        customer.setRole(Role.STAFF);
        when(accountRepo.findFirstByUsername("doctor1")).thenReturn(Optional.of(customer));
        when(passwords.matches("secret", "hash")).thenReturn(true);
        StaffInfo doctor = StaffInfo.builder().staffId(UUID.randomUUID()).systemRole(SystemRole.DOCTOR).build();
        when(staff.findFirstByProfile_Account_Username(customer.getUsername())).thenReturn(Optional.of(doctor));
        tokens(customer);
        var result = service.login(new LoginRequest("doctor1", "secret"));
        assertEquals("DOCTOR", result.account().systemRole());
        verify(jwt).generateAccessToken(customer, doctor.getStaffId(), SystemRole.DOCTOR);
    }

    @Test void oldCustomerUsernameCannotBypassCurrentContact() {
        when(redis.opsForValue()).thenReturn(values);
        when(accountRepo.findFirstByUsername("old@example.test")).thenReturn(Optional.of(customer));
        when(values.increment("auth:login-attempt:old@example.test")).thenReturn(1L);
        assertThrows(BadRequestException.class, () -> service.login(new LoginRequest("old@example.test", "secret")));
        verifyNoInteractions(passwords, jwt);
        verify(redis).expire("auth:login-attempt:old@example.test", 15L, TimeUnit.MINUTES);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "nobody"})
    void unknownIdentityHasGenericFailureAndNoToken(String identifier) {
        when(redis.opsForValue()).thenReturn(values);
        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.login(new LoginRequest(identifier, "secret")));
        assertEquals("Tên đăng nhập hoặc mật khẩu không đúng", error.getMessage());
        verifyNoInteractions(jwt);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {1L, 2L})
    void wrongPasswordRecordsAttemptButOnlyFirstSetsExpiry(Long attempt) {
        loginLookup();
        when(values.increment("auth:login-attempt:user@example.test")).thenReturn(attempt);
        assertThrows(BadRequestException.class, () -> service.login(new LoginRequest("user@example.test", "wrong")));
        verify(redis, times(Long.valueOf(1).equals(attempt) ? 1 : 0))
                .expire(anyString(), eq(15L), eq(TimeUnit.MINUTES));
        verifyNoInteractions(jwt);
    }

    @Test void lockedAccountNeverReceivesTokensEvenWithCorrectPassword() {
        loginLookup();
        customer.setIsActive(false);
        when(passwords.matches("secret", "hash")).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.login(new LoginRequest("user@example.test", "secret")));
        verifyNoInteractions(jwt);
        verify(redis, never()).delete(anyString());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1L, 0L, 7L})
    void rateLimitStopsBeforeAccountLookupAndShowsPositiveDelay(Long ttl) {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:login-attempt:0900000000")).thenReturn("5");
        when(redis.getExpire("auth:login-attempt:0900000000", TimeUnit.MINUTES)).thenReturn(ttl);
        var error = assertThrows(BadRequestException.class,
                () -> service.login(new LoginRequest("+84900000000", "secret")));
        assertTrue(error.getMessage().contains((ttl != null && ttl > 1 ? ttl : 1) + " phút"));
        verifyNoInteractions(profiles, accountRepo, passwords, jwt);
    }

    @Test void malformedAttemptCounterIsRemoved() {
        loginLookup();
        when(values.get("auth:login-attempt:user@example.test")).thenReturn("broken");
        assertThrows(BadRequestException.class, () -> service.login(new LoginRequest("user@example.test", "wrong")));
        verify(redis).delete("auth:login-attempt:user@example.test");
    }

    @Test void redisReadAndWriteFailureDoesNotHideCredentialError() {
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("isolated test"));
        var error = assertThrows(BadRequestException.class,
                () -> service.login(new LoginRequest("nobody", "wrong")));
        assertEquals("Tên đăng nhập hoặc mật khẩu không đúng", error.getMessage());
        verifyNoInteractions(jwt);
    }

    @Test void redisDeleteFailureStillAllowsValidLogin() {
        loginLookup();
        when(passwords.matches("secret", "hash")).thenReturn(true);
        when(redis.delete(anyString())).thenThrow(new RedisConnectionFailureException("isolated test"));
        tokens(customer);
        assertEquals("access", service.login(new LoginRequest("user@example.test", "secret")).accessToken());
    }

    @Test void unavailableLockTtlFallsBackToOneMinute() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn("6");
        when(redis.getExpire(anyString(), eq(TimeUnit.MINUTES)))
                .thenThrow(new RedisConnectionFailureException("isolated test"));
        assertTrue(assertThrows(BadRequestException.class,
                () -> service.login(new LoginRequest("doctor1", "secret"))).getMessage().contains("1 phút"));
    }

    @Test void refreshRequiresRefreshTokenType() {
        Claims claims = mock(Claims.class);
        when(jwt.parseClaims("token")).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("access");
        assertThrows(BadRequestException.class, () -> service.refresh(new RefreshRequest("token")));
        verifyNoInteractions(accounts);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void refreshOnlyIssuesTokensForActiveAccount(boolean active) {
        Claims claims = mock(Claims.class);
        when(jwt.parseClaims("token")).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("refresh");
        when(claims.getSubject()).thenReturn(customer.getUsername());
        when(accounts.findByUsername(customer.getUsername())).thenReturn(customer);
        customer.setIsActive(active);
        if (active) {
            tokens(customer);
            assertEquals("refresh", service.refresh(new RefreshRequest("token")).refreshToken());
        } else {
            assertThrows(BadRequestException.class, () -> service.refresh(new RefreshRequest("token")));
            verify(jwt, never()).generateAccessToken(any(), any(), any());
        }
    }

    @Test void malformedRefreshTokenIsTranslatedToBusinessError() {
        when(jwt.parseClaims("bad")).thenThrow(new JwtException("bad signature"));
        assertTrue(assertThrows(BadRequestException.class,
                () -> service.refresh(new RefreshRequest("bad"))).getMessage().contains("hết hạn"));
    }

    @Test void anonymousContextHasNoStaffRoleOrAccount() {
        assertNull(service.currentStaffId());
        assertNull(service.getCurrentSystemRole());
        assertThrows(BadRequestException.class, service::currentAccount);
        authentication(Map.of());
        assertNull(service.currentStaffId());
        assertThrows(BadRequestException.class, service::currentAccount);
    }

    @Test void mapPrincipalResolvesAccountAndProfile() {
        authentication(Map.of("username", customer.getUsername(), "staffId", UUID.randomUUID().toString(), "systemRole", "DOCTOR"));
        when(accounts.findByUsername(customer.getUsername())).thenReturn(customer);
        when(profiles.findFirstByAccount_AccountId(customer.getAccountId())).thenReturn(Optional.of(profile));
        assertSame(customer, service.currentAccount());
        assertEquals(profile.getProfileId(), service.currentProfileId());
        assertNotNull(service.currentStaffId());
        assertEquals(SystemRole.DOCTOR, service.getCurrentSystemRole());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid-uuid"})
    void legacyStaffClaimFallsBackToUsername(String staffId) {
        authentication(Map.of("username", "doctor1", "staffId", staffId));
        UUID expected = UUID.randomUUID();
        when(staff.findFirstByProfile_Account_Username("doctor1"))
                .thenReturn(Optional.of(StaffInfo.builder().staffId(expected).build()));
        assertEquals(expected, service.currentStaffId());
    }

    @ParameterizedTest
    @EnumSource(value = SystemRole.class, names = {"CLINIC_MANAGER", "ADMIN", "NURSE", "RECEPTIONIST", "CASHIER", "DOCTOR"})
    void roleCanBeResolvedFromAuthorities(SystemRole role) {
        authentication("staff", "ROLE_" + role.name());
        assertEquals(role, service.getCurrentSystemRole());
        assertNull(service.currentStaffId());
    }

    @Test void customerHasNoStaffRoleAndPasswordChangeUsesCurrentAccount() {
        authentication(customer.getUsername(), "ROLE_CUSTOMER");
        when(accounts.findByUsername(customer.getUsername())).thenReturn(customer);
        assertNull(service.getCurrentSystemRole());
        service.changeMyPassword(new ChangePasswordRequest("old", "new-secret"));
        verify(accounts).changePassword(customer.getAccountId(), "old", "new-secret");
    }

    @Test void resetRequiresOtpAndUsesCurrentContactAccount() {
        var req = new ResetPasswordRequest("user@example.test", "123456", "new-secret");
        assertThrows(BadRequestException.class, () -> service.resetPassword(req));
        verifyNoInteractions(accounts, profiles);
        when(otp.verifyOtp(req.identifier(), req.otp())).thenReturn(true);
        when(profiles.findFirstByEmailIgnoreCase(req.identifier())).thenReturn(Optional.of(profile));
        service.resetPassword(req);
        verify(accounts).adminResetPassword(customer.getAccountId(), "new-secret");
    }

    private RegisterRequest registration(String identifier, Gender gender, LocalDate dob) {
        return new RegisterRequest(identifier, "secret-123", "Nguyễn Minh An", dob, gender, null, null, "Hà Nội");
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = Gender.class, names = "OTHER")
    void registrationRequiresSupportedGenderBeforeOtp(Gender gender) {
        assertThrows(BadRequestException.class,
                () -> service.register(registration("user@example.test", gender, LocalDate.of(2000, 1, 1))));
        verifyNoInteractions(otp, accounts, jwt);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2999-01-01", "1600-01-01"})
    void impossibleBirthDateIsRejectedBeforeOtp(String date) {
        assertThrows(BadRequestException.class,
                () -> service.register(registration("user@example.test", Gender.MALE, LocalDate.parse(date))));
        verifyNoInteractions(otp, accounts, jwt);
    }

    @Test void unverifiedOtpCannotCreateAccount() {
        var request = registration("user@example.test", Gender.FEMALE, null);
        assertThrows(BadRequestException.class, () -> service.register(request));
        verify(otp).isOtpVerified(request.identifier());
        verifyNoInteractions(accounts, jwt);
        verify(profiles, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"user@example.test,true", "user@example.test,false", "+84900000000,true", "+84900000000,false"})
    void registerCreatesOrReusesGuestProfileAndConsumesOtp(String identifier, boolean reuseGuest) {
        boolean email = identifier.contains("@");
        Profile guest = Profile.builder().profileId(UUID.randomUUID()).fullName("Tên hồ sơ cũ").build();
        if (reuseGuest) {
            if (email) {
                when(profiles.findFirstByEmailIgnoreCase(identifier)).thenReturn(Optional.of(guest));
                when(profiles.findFirstByEmail(identifier)).thenReturn(Optional.of(guest));
            } else when(profiles.findFirstByPhoneIn(anySet())).thenReturn(Optional.of(guest));
        }
        when(otp.isOtpVerified(identifier)).thenReturn(true);
        when(accounts.create(identifier, "secret-123", Role.CUSTOMER)).thenReturn(customer);
        when(profiles.save(any())).thenAnswer(inv -> {
            Profile saved = inv.getArgument(0);
            if (saved.getProfileId() == null) saved.setProfileId(UUID.randomUUID());
            return saved;
        });
        tokens(customer);
        var result = service.register(registration(identifier, Gender.FEMALE, LocalDate.of(2000, 1, 1)));

        ArgumentCaptor<Profile> saved = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(saved.capture());
        assertSame(customer, saved.getValue().getAccount());
        assertEquals("Nguyễn Minh An", saved.getValue().getFullName());
        assertEquals(Gender.FEMALE, saved.getValue().getGender());
        assertEquals("Hà Nội", saved.getValue().getAddress());
        if (reuseGuest) assertSame(guest, saved.getValue());
        if (email) assertEquals(identifier, saved.getValue().getEmail());
        else assertEquals("0900000000", saved.getValue().getPhone());
        assertEquals("access", result.accessToken());
        verify(otp).consumeVerifiedOtp(identifier);
        verify(appointments).findGuestAppointmentsByPhonesOrEmails(
                argThat(phones -> email ? phones.equals(Set.of("INVALID_DUMMY_PHONE")) : phones.contains("0900000000")),
                argThat(emails -> email ? emails.equals(Set.of(identifier)) : emails.equals(Set.of("INVALID_DUMMY_EMAIL"))));
    }

    @Test void registrationLinksExistingGuestVisitAndInvoiceToVerifiedProfile() {
        String email = "user@example.test";
        Appointment appointment = Appointment.builder().appointmentId(UUID.randomUUID()).isGuest(true).build();
        Appointment withoutVisit = Appointment.builder().appointmentId(UUID.randomUUID()).isGuest(true).build();
        CustomerVisit visit = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        Invoice invoice = Invoice.builder().invoiceId(UUID.randomUUID()).build();
        when(otp.isOtpVerified(email)).thenReturn(true);
        when(accounts.create(email, "secret-123", Role.CUSTOMER)).thenReturn(customer);
        when(profiles.save(any())).thenReturn(profile);
        when(appointments.findGuestAppointmentsByPhonesOrEmails(anySet(), anySet())).thenReturn(List.of(appointment, withoutVisit));
        when(visits.findByAppointment_AppointmentId(appointment.getAppointmentId())).thenReturn(Optional.of(visit));
        when(invoices.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(invoice));
        tokens(customer);
        service.register(registration(email, Gender.MALE, null));
        assertSame(profile, appointment.getCustomer());
        assertFalse(appointment.getIsGuest());
        assertSame(profile, withoutVisit.getCustomer());
        assertSame(profile, visit.getCustomer());
        assertSame(profile, invoice.getCustomer());
        verify(visits).save(visit);
        verify(invoices).saveAll(List.of(invoice));
        verify(otp).consumeVerifiedOtp(email);
    }

    @Test void phoneAlreadyLinkedCannotRegisterAgain() {
        when(profiles.findFirstByPhoneIn(anySet())).thenReturn(Optional.of(profile));
        var error = assertThrows(BadRequestException.class,
                () -> service.register(registration("0900000000", Gender.FEMALE, null)));
        assertTrue(error.getMessage().startsWith("Số điện thoại"));
        verifyNoInteractions(otp, accounts, jwt);
    }
}
