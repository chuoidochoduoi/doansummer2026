package org.example.doansummer2026.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.config.JwtService;
import org.example.doansummer2026.dto.auth.AuthResponse;
import org.example.doansummer2026.dto.auth.ChangePasswordRequest;
import org.example.doansummer2026.dto.auth.LoginRequest;
import org.example.doansummer2026.dto.auth.RefreshRequest;
import org.example.doansummer2026.dto.auth.RegisterRequest;
import org.example.doansummer2026.dto.auth.ResetPasswordRequest;
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.service.interfaces.AuthServiceInterface;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class AuthService implements AuthServiceInterface {

    private final AccountService accountService;
    private final ProfileRepository profileRepository;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final StaffInfoRepository staffRepo;
    private final AppointmentRepository appointmentRepository;
    private final CustomerVisitRepository visitRepository;
    private final InvoiceRepository invoiceRepository;
    private final AccountRepository accountRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.auth.max-login-attempts:5}")
    private long maxLoginAttempts;
    @Value("${app.auth.login-lock-minutes:15}")
    private long loginLockMinutes;
    public AuthResponse register(RegisterRequest req) {

        ensureRegistrationIdentifierAvailable(req.identifier());

        if (req.gender() == null
                || (req.gender() != org.example.doansummer2026.enums.Gender.MALE
                && req.gender() != org.example.doansummer2026.enums.Gender.FEMALE)) {
            throw new BadRequestException("Giới tính chỉ được chọn Nam hoặc Nữ");
        }

        // 1. Kiểm tra ngày sinh hợp lệ
        if (req.dob() != null) {
            java.time.LocalDate today = java.time.LocalDate.now();
            if (req.dob().isAfter(today)) {
                throw new BadRequestException("Ngày sinh không thể ở trong tương lai");
            }
            if (java.time.temporal.ChronoUnit.YEARS.between(req.dob(), today) > 150) {
                throw new BadRequestException("Tuổi không hợp lệ (lớn hơn 150 tuổi)");
            }
        }

        // 2. OTP phải được xác thực trước
        if (!otpService.isOtpVerified(req.identifier())) {
            throw new BadRequestException("Vui lòng xác thực OTP trước khi đăng ký");
        }

        String identifier = req.identifier().trim();

        boolean registerByEmail = identifier.contains("@");

        String email = null;
        String phone = null;

        if (registerByEmail) {
            email = identifier.toLowerCase();
        } else {
            phone = normalizePhone(identifier);
        }

        Profile profile;

        // =========================================================
        // ĐĂNG KÝ BẰNG EMAIL
        // =========================================================
        if (registerByEmail) {

            Profile emailProfile = profileRepository
                    .findFirstByEmail(email)
                    .orElse(null);

            if (emailProfile != null && emailProfile.getAccount() != null) {
                throw new BadRequestException(
                        "Email đã được liên kết với tài khoản khác"
                );
            }

            if (emailProfile != null) {
                profile = emailProfile;
            } else {
                profile = new Profile();
            }

        }
        // =========================================================
        // ĐĂNG KÝ BẰNG SỐ ĐIỆN THOẠI
        // =========================================================
        else {

            Set<String> phones = phoneVariants(phone);

            Profile phoneProfile = profileRepository
                    .findFirstByPhoneIn(phones)
                    .orElse(null);

            if (phoneProfile != null && phoneProfile.getAccount() != null) {
                throw new BadRequestException(
                        "Số điện thoại đã được liên kết với tài khoản khác"
                );
            }

            if (phoneProfile != null) {
                profile = phoneProfile;
            } else {
                profile = new Profile();
            }
        }

        // =========================================================
        // 2. TẠO ACCOUNT
        // identifier chính là email hoặc số điện thoại đã verify OTP
        // =========================================================
        Account account = accountService.create(
                identifier,
                req.password(),
                Role.CUSTOMER
        );

        // =========================================================
        // 3. GÁN ACCOUNT + THÔNG TIN HỒ SƠ
        // =========================================================
        profile.setAccount(account);

        if (registerByEmail) {
            profile.setEmail(email);
        } else {
            profile.setPhone(phone);
        }

        profile.setFullName(req.fullName());
        profile.setDateOfBirth(req.dob());
        profile.setGender(req.gender());
        profile.setAddress(req.address());

        profile = profileRepository.save(profile);

        // =========================================================
        // 4. LINK LỊCH SỬ GUEST
        // =========================================================
        if (registerByEmail) {

            linkGuestHistory(
                    profile,
                    Set.of("INVALID_DUMMY_PHONE"),
                    Set.of(email)
            );

        } else {

            linkGuestHistory(
                    profile,
                    phoneVariants(phone),
                    Set.of("INVALID_DUMMY_EMAIL")
            );
        }

        // =========================================================
        // 5. OTP CHỈ DÙNG 1 LẦN
        // =========================================================
        otpService.consumeVerifiedOtp(identifier);

        // =========================================================
        // 6. AUTO LOGIN
        // =========================================================
        return buildAuthResponse(account);
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> registrationAvailability(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        boolean validPhone = value.matches("^(\\+84|0)\\d{9,10}$");
        boolean validEmail = value.matches("^[\\w\\-.]+@([\\w-]+\\.)+[\\w-]{2,}$");
        if (!validPhone && !validEmail) {
            throw new BadRequestException("Email hoặc số điện thoại không hợp lệ");
        }
        Profile existing;
        if (value.contains("@")) {
            existing = profileRepository.findFirstByEmailIgnoreCase(value).orElse(null);
        } else {
            existing = profileRepository.findFirstByPhoneIn(phoneVariants(value)).orElse(null);
        }
        boolean hasProfile = existing != null;
        boolean registered = hasProfile && existing.getAccount() != null;
        return Map.of(
                "exists", hasProfile,
                "registered", registered,
                "available", !registered
        );
    }

    @Transactional(readOnly = true)
    public void ensureRegistrationIdentifierAvailable(String identifier) {
        Map<String, Boolean> availability = registrationAvailability(identifier);
        if (Boolean.TRUE.equals(availability.get("registered"))) {
            String label = identifier != null && identifier.contains("@") ? "Email" : "Số điện thoại";
            throw new BadRequestException(label + " đã được liên kết với một tài khoản");
        }
    }

    private void linkGuestHistory(Profile profile, Set<String> phones, Set<String> emails) {
        var appointments = appointmentRepository.findGuestAppointmentsByPhonesOrEmails(phones, emails);
        for (var appointment : appointments) {
            appointment.setCustomer(profile);
            appointment.setIsGuest(false);
            appointmentRepository.save(appointment);
            visitRepository.findByAppointment_AppointmentId(appointment.getAppointmentId()).ifPresent(visit -> {
                visit.setCustomer(profile);
                visitRepository.save(visit);
                var invoices = invoiceRepository.findAllByVisit_VisitId(visit.getVisitId());
                invoices.forEach(invoice -> invoice.setCustomer(profile));
                invoiceRepository.saveAll(invoices);
            });
        }
    }

    private String normalizePhone(String phone) {

        if (phone == null || phone.isBlank()) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("84") && digits.length() >= 11) {
            return "0" + digits.substring(2);
        }
        return digits;
    }

    private Set<String> phoneVariants(String phone) {
        String normalized = normalizePhone(phone);

        Set<String> variants = new LinkedHashSet<>();

        if (phone != null && !phone.isBlank()) {
            variants.add(phone.trim());
        }

        if (!normalized.isBlank()) {
            variants.add(normalized);
        }

        if (normalized.startsWith("0") && normalized.length() > 1) {
            variants.add("84" + normalized.substring(1));
            variants.add("+84" + normalized.substring(1));
        }

        return variants;
    }

    /**
     * Khách hàng đăng nhập bằng email hoặc số điện thoại hiện đang lưu trên
     * Profile. account.username chỉ còn là định danh nội bộ sau khi thông tin
     * liên hệ thay đổi. Nhân viên vẫn được đăng nhập bằng username nghiệp vụ.
     */
    private Account resolveLoginAccount(String rawIdentifier) {
        String identifier = rawIdentifier == null ? "" : rawIdentifier.trim();
        if (identifier.isBlank()) {
            throw invalidCredentials();
        }

        Profile contactProfile;
        if (identifier.contains("@")) {
            contactProfile = profileRepository.findFirstByEmailIgnoreCase(identifier).orElse(null);
        } else if (identifier.matches("^(\\+84|0)\\d{9,10}$")) {
            contactProfile = profileRepository.findFirstByPhoneIn(phoneVariants(identifier)).orElse(null);
        } else {
            contactProfile = null;
        }

        if (contactProfile != null && contactProfile.getAccount() != null) {
            return contactProfile.getAccount();
        }

        // Không cho tài khoản CUSTOMER tiếp tục đăng nhập bằng email/SĐT cũ
        // còn lưu trong account.username. Fallback này chỉ dành cho username
        // độc lập của nhân viên như admin, receptionist1, doctor1...
        Account usernameAccount = accountRepository.findFirstByUsername(identifier).orElse(null);
        if (usernameAccount != null && usernameAccount.getRole() == Role.STAFF) {
            return usernameAccount;
        }
        throw invalidCredentials();
    }

    private BadRequestException invalidCredentials() {
        return new BadRequestException("Tên đăng nhập hoặc mật khẩu không đúng");
    }

    public AuthResponse login(LoginRequest req) {
        String attemptKey = "auth:login-attempt:" + normalizeLoginIdentifier(req.username());
        long previousAttempts = readLoginAttempts(attemptKey);
        if (previousAttempts >= maxLoginAttempts) {
            Long ttl = readLoginAttemptTtl(attemptKey);
            throw new BadRequestException("Đăng nhập sai quá nhiều lần. Vui lòng thử lại sau "
                    + Math.max(ttl == null ? 1 : ttl, 1) + " phút");
        }

        Account account;
        try {
            account = resolveLoginAccount(req.username());
        } catch (BadRequestException ex) {
            registerFailedLogin(attemptKey);
            throw ex;
        }

        if (!passwordEncoder.matches(req.password(), account.getPasswordHash())) {
            registerFailedLogin(attemptKey);
            throw new BadRequestException("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        if (!account.getIsActive()) {
            throw new BadRequestException("Tài khoản đã bị khóa");
        }

        clearLoginAttempts(attemptKey);
        return buildAuthResponse(account);
    }

    private void registerFailedLogin(String attemptKey) {
        try {
            Long attempts = redisTemplate.opsForValue().increment(attemptKey);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(attemptKey, loginLockMinutes, TimeUnit.MINUTES);
            }
        } catch (org.springframework.data.redis.RedisConnectionFailureException ex) {
            log.warn("Redis unavailable; login attempt could not be recorded");
        }
    }

    private long readLoginAttempts(String attemptKey) {
        try {
            String value = redisTemplate.opsForValue().get(attemptKey);
            if (value == null) return 0L;
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                redisTemplate.delete(attemptKey);
                return 0L;
            }
        } catch (org.springframework.data.redis.RedisConnectionFailureException ex) {
            log.warn("Redis unavailable; login rate limit check skipped");
            return 0L;
        }
    }

    private Long readLoginAttemptTtl(String attemptKey) {
        try {
            return redisTemplate.getExpire(attemptKey, TimeUnit.MINUTES);
        } catch (org.springframework.data.redis.RedisConnectionFailureException ex) {
            log.warn("Redis unavailable while reading login lock TTL");
            return 1L;
        }
    }

    private void clearLoginAttempts(String attemptKey) {
        try {
            redisTemplate.delete(attemptKey);
        } catch (org.springframework.data.redis.RedisConnectionFailureException ex) {
            log.warn("Redis unavailable; login attempt counter was not cleared");
        }
    }

    private String normalizeLoginIdentifier(String identifier) {
        if (identifier == null) return "empty";
        String normalized = identifier.trim().toLowerCase();
        if (!normalized.contains("@") && normalized.matches("^(\\+84|0)\\d{9,10}$")) {
            normalized = normalizePhone(normalized);
        }
        return normalized.isBlank() ? "empty" : normalized;
    }

    public AuthResponse refresh(RefreshRequest req) {
        try {
            Claims claims = jwtService.parseClaims(req.refreshToken());
            String type = claims.get("type", String.class);
            if (!"refresh".equals(type)) {
                throw new BadRequestException("Token không phải là refresh token");
            }
            String username = claims.getSubject();
            Account account = accountService.findByUsername(username);
            if (!account.getIsActive()) {
                throw new BadRequestException("Tài khoản đã bị khóa");
            }
            return buildAuthResponse(account);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadRequestException("Refresh token không hợp lệ hoặc đã hết hạn");
        }
    }

    @Transactional(readOnly = true)
    public Account currentAccount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BadRequestException("Chưa xác thực");
        }
        String username;
        Object principal = auth.getPrincipal();
        if (principal instanceof Map<?, ?> map) {
            username = (String) map.get("username");
        } else {
            username = auth.getName();
        }
        if (username == null) {
            throw new BadRequestException("Chưa xác thực");
        }
        return accountService.findByUsername(username);
    }

    public void changeMyPassword(ChangePasswordRequest req) {
        Account me = currentAccount();
        accountService.changePassword(me.getAccountId(), req.oldPassword(), req.newPassword());
    }

    /** Lay profileId cua user dang dang nhap (chi cho CUSTOMER). */
    @Transactional(readOnly = true)
    public UUID currentProfileId() {
        Account account = currentAccount();
        if (account != null) {
            return profileRepository.findFirstByAccount_AccountId(account.getAccountId()).map(p -> p.getProfileId()).orElse(null);
        }
        return null;
    }

    /** Lay staffId cua user dang dang nhap (null neu khong phai staff). */
    @Transactional(readOnly = true)
    public UUID currentStaffId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Map<?, ?> map) {
            Object staffId = map.get("staffId");
            if (staffId instanceof String sid && !sid.isBlank()) {
                try {
                    return UUID.fromString(sid);
                } catch (IllegalArgumentException ignored) {
                    // Token cu/khong hop le: tra lai theo username ben duoi.
                }
            }
            Object username = map.get("username");
            if (username instanceof String value && !value.isBlank()) {
                return staffRepo.findFirstByProfile_Account_Username(value)
                        .map(staff -> staff.getStaffId()).orElse(null);
            }
        }
        return null;
    }

    /** Lay systemRole cua user dang dang nhap (null neu khong phai staff). */
    @Transactional(readOnly = true)
    public SystemRole getCurrentSystemRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Map<?, ?> map) {
            String sr = (String) map.get("systemRole");
            if (sr != null) return SystemRole.valueOf(sr);
        }
        // JWT luu danh sach authorities thay vi claim systemRole. Suy ra vai tro
        // tu authority de cac API quan ly khong bi nham thanh bac si thuong.
        var authorities = auth.getAuthorities().stream().map(item -> item.getAuthority()).collect(java.util.stream.Collectors.toSet());
        if (authorities.contains("ROLE_CLINIC_MANAGER")) return SystemRole.CLINIC_MANAGER;
        if (authorities.contains("ROLE_ADMIN")) return SystemRole.ADMIN;
        if (authorities.contains("ROLE_NURSE")) return SystemRole.NURSE;
        if (authorities.contains("ROLE_RECEPTIONIST")) return SystemRole.RECEPTIONIST;
        if (authorities.contains("ROLE_CASHIER")) return SystemRole.CASHIER;
        if (authorities.contains("ROLE_DOCTOR")) return SystemRole.DOCTOR;
        return null;
    }

    private AuthResponse buildAuthResponse(Account account) {
        // Tim staffId va systemRole tu account (chi co cho staff, customer tra ve null)
        var staffOpt = staffRepo.findFirstByProfile_Account_Username(account.getUsername());
        UUID staffId = staffOpt.map(staff -> staff.getStaffId()).orElse(null);
        SystemRole systemRole = staffOpt.map(staff -> staff.getSystemRole()).orElse(null);

        String access = jwtService.generateAccessToken(account, staffId, systemRole);
        String refresh = jwtService.generateRefreshToken(account, staffId, systemRole);
        return new AuthResponse(
                access,
                refresh,
                "Bearer",
                jwtService.getAccessExpirationMs() / 1000L,
                new AuthResponse.AccountInfo(
                        account.getAccountId(),
                        account.getUsername(),
                        account.getRole().name(),
                        systemRole != null ? systemRole.name() : null)
        );
    }

    public void resetPassword(org.example.doansummer2026.dto.auth.ResetPasswordRequest req) {
        // Xac thuc OTP
        if (!otpService.verifyOtp(req.identifier(), req.otp())) {
            throw new BadRequestException("OTP không hợp lệ hoặc đã hết hạn");
        }
        // Tim tai khoan
        Account account = resolveLoginAccount(req.identifier());
        if (account == null) {
            throw new BadRequestException("Tài khoản không tồn tại");
        }
        accountService.adminResetPassword(account.getAccountId(), req.newPassword());
    }
}
