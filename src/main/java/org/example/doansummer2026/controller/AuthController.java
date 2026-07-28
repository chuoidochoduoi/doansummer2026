package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.auth.AuthResponse;
import org.example.doansummer2026.dto.auth.ChangePasswordRequest;
import org.example.doansummer2026.dto.auth.LoginRequest;
import org.example.doansummer2026.dto.auth.RefreshRequest;
import org.example.doansummer2026.dto.auth.RegisterRequest;
import org.example.doansummer2026.dto.auth.SendOtpRequest;
import org.example.doansummer2026.dto.account.AccountResponse;
import org.example.doansummer2026.service.AuthService;
import org.example.doansummer2026.service.OtpService;
import org.example.doansummer2026.service.interfaces.AuthServiceInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceInterface authService;
    private final OtpService otpService;

    /** Public: dang ky benh nhan moi -> 200 OK (tra token, khong tao resource co URI rieng). */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return RestResponses.ok(authService.register(req));
    }

    /** Public: gui OTP xac thuc SĐT -> 200 OK (OTP in-memory, dev log ra console). */
    @PostMapping("/send-otp")
    public ResponseEntity<Void> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        otpService.sendOtp(req.phone());
        return RestResponses.ok(null);
    }

    /** Public: dang nhap, tra access + refresh token -> 200 OK. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return RestResponses.ok(authService.login(req));
    }

    /** Public: cap lai access token tu refresh token hop le -> 200 OK. */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return RestResponses.ok(authService.refresh(req));
    }

    /** Authenticated: thong tin tai khoan hien tai. */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountResponse> me() {
        var account = authService.currentAccount();
        var systemRole = authService.getCurrentSystemRole();
        return RestResponses.ok(AccountResponse.from(account, systemRole));
    }

    /** Authenticated: doi mat khau -> 204 No Content. */
    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changeMyPassword(req);
        return RestResponses.noContent();
    }
}



