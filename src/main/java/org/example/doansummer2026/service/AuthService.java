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
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
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
    public AuthResponse register(RegisterRequest req) {
        // Xac thuc OTP truoc khi dang ky
        if (!otpService.verifyOtp(req.phone(), req.otp())) {
            throw new BadRequestException("OTP khong hop le hoac da het han");
        }

        // Tao account voi role CUSTOMER
        Account account = accountService.create(req.phone(), req.password(), Role.CUSTOMER);

        // Tao profile lien ket
        Profile profile = Profile.builder()
                .account(account)
                .fullName("User " + UUID.randomUUID().toString().substring(0, 8))
                .phone(req.phone())
                .build();
        profileRepository.save(profile);
        return buildAuthResponse(account);
    }

    public AuthResponse login(LoginRequest req) {
        Account account = accountService.findByUsername(req.username());

        if (!passwordEncoder.matches(req.password(), account.getPasswordHash())) {
            throw new BadRequestException("Username hoac password khong dung");
        }

        if (!account.getIsActive()) {
            throw new BadRequestException("Tai khoan da bi khoa");
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
            return profileRepository.findByAccount_AccountId(account.getAccountId()).map(p -> p.getProfileId()).orElse(null);
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
            return sr != null ? SystemRole.valueOf(sr) : null;
        }
        return null;
    }

    private AuthResponse buildAuthResponse(Account account) {
        // Tim staffId va systemRole tu account (chi co cho staff, customer tra ve null)
        var staffOpt = staffRepo.findByProfile_Account_Username(account.getUsername());
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
}



