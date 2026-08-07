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
import org.example.doansummer2026.service.interfaces.AuthServiceInterface;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
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
    public AuthResponse register(RegisterRequest req) {
        // Xac thuc OTP truoc khi dang ky
        if (!otpService.verifyOtp(req.identifier(), req.otp())) {
            throw new BadRequestException("OTP khong hop le hoac da het han");
        }

        // Tao account voi role CUSTOMER
        Account account = accountService.create(req.identifier(), req.password(), Role.CUSTOMER);

        Profile profile;
        if (req.identifier().contains("@")) {
            profile = profileRepository.findFirstByEmail(req.identifier()).orElse(null);
            if (profile != null && profile.getAccount() != null) {
                throw new BadRequestException("Email da duoc lien ket voi tai khoan khac");
            }
            if (profile == null) {
                profile = Profile.builder().account(account).email(req.identifier()).build();
            } else {
                profile.setAccount(account);
            }
            linkGuestHistory(profile, profile.getPhone() != null ? phoneVariants(profile.getPhone()) : Set.of("INVALID_DUMMY_PHONE"), Set.of(req.identifier()));
        } else {
            Set<String> phoneVariants = phoneVariants(req.identifier());
            profile = profileRepository.findFirstByPhoneIn(phoneVariants).orElse(null);
            if (profile != null && profile.getAccount() != null) {
                throw new BadRequestException("So dien thoai da duoc lien ket voi tai khoan khac");
            }
            if (profile == null) {
                profile = Profile.builder().account(account).phone(normalizePhone(req.identifier())).build();
            } else {
                profile.setAccount(account);
                profile.setPhone(normalizePhone(req.identifier()));
            }
            linkGuestHistory(profile, phoneVariants, profile.getEmail() != null ? Set.of(profile.getEmail()) : Set.of("INVALID_DUMMY_EMAIL"));
        }
        
        profile = profileRepository.save(profile);
        return buildAuthResponse(account);
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
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if (digits.startsWith("84") && digits.length() >= 11) digits = "0" + digits.substring(2);
        return digits;
    }

    private Set<String> phoneVariants(String phone) {
        String normalized = normalizePhone(phone);
        Set<String> variants = new LinkedHashSet<>();
        variants.add(phone);
        variants.add(normalized);
        if (normalized.startsWith("0")) {
            variants.add("84" + normalized.substring(1));
            variants.add("+84" + normalized.substring(1));
        }
        return variants;
    }

    public AuthResponse login(LoginRequest req) {
        Account account = accountService.findByUsername(req.username());

        if (!passwordEncoder.matches(req.password(), account.getPasswordHash())) {
            throw new BadRequestException("Username hoac password khong dung");
        }

        if (!account.getIsActive()) {
            throw new BadRequestException("Tai khoan da bi khoa");
        }

        return buildAuthResponse(account);
    }

    public AuthResponse refresh(RefreshRequest req) {
        try {
            Claims claims = jwtService.parseClaims(req.refreshToken());
            String type = claims.get("type", String.class);
            if (!"refresh".equals(type)) {
                throw new BadRequestException("Token khong phai refresh token");
            }
            String username = claims.getSubject();
            Account account = accountService.findByUsername(username);
            if (!account.getIsActive()) {
                throw new BadRequestException("Tai khoan da bi khoa");
            }
            return buildAuthResponse(account);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadRequestException("Refresh token khong hop le hoac het han");
        }
    }

    @Transactional(readOnly = true)
    public Account currentAccount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BadRequestException("Chua xac thuc");
        }
        String username;
        Object principal = auth.getPrincipal();
        if (principal instanceof Map<?, ?> map) {
            username = (String) map.get("username");
        } else {
            username = auth.getName();
        }
        if (username == null) {
            throw new BadRequestException("Chua xac thuc");
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
            String sid = (String) map.get("staffId");
            return sid != null ? UUID.fromString(sid) : null;
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
            throw new BadRequestException("OTP khong hop le hoac da het han");
        }
        // Tim tai khoan
        Account account = accountService.findByUsername(req.identifier());
        if (account == null) {
            throw new BadRequestException("Tai khoan khong ton tai");
        }
        accountService.adminResetPassword(account.getAccountId(), req.newPassword());
    }
}

