package vn.edu.fpt.cares.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.RestResponses;
import vn.edu.fpt.cares.dto.auth.AuthResponse;
import vn.edu.fpt.cares.dto.auth.ChangePasswordRequest;
import vn.edu.fpt.cares.dto.auth.LoginRequest;
import vn.edu.fpt.cares.dto.auth.RefreshRequest;
import vn.edu.fpt.cares.dto.auth.RegisterRequest;
import vn.edu.fpt.cares.dto.auth.ResetPasswordRequest;
import vn.edu.fpt.cares.dto.auth.SendOtpRequest;
import vn.edu.fpt.cares.dto.auth.SendOtpResponse;
import vn.edu.fpt.cares.dto.account.AccountResponse;
import vn.edu.fpt.cares.service.AuthService;
import vn.edu.fpt.cares.service.OtpService;
import vn.edu.fpt.cares.service.interfaces.AuthServiceInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceInterface authService;
    private final OtpService otpService;

    @Value("${app.otp.expose-code:false}")
    private boolean exposeOtpCode;

    /** Public: dang ky benh nhan moi -> 200 OK (tra token, khong tao resource co URI rieng). */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return RestResponses.ok(authService.register(req));
    }

    /** Public: gui OTP xac thuc SĐT -> 200 OK (OTP in-memory, dev log ra console). */
    @PostMapping("/send-otp")
    public ResponseEntity<SendOtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        String code = otpService.sendOtp(req.identifier());
        return RestResponses.ok(new SendOtpResponse(exposeOtpCode ? code : null, 300));
    }

    /** Kiểm tra số điện thoại/email trước khi đăng ký, không chặn hồ sơ khách vãng lai chưa có tài khoản. */
    @GetMapping("/registration-availability")
    public ResponseEntity<java.util.Map<String, Boolean>> registrationAvailability(
            @RequestParam String identifier) {
        return RestResponses.ok(authService.registrationAvailability(identifier));
    }

    /** Gửi OTP riêng cho đăng ký; chặn identifier đã có tài khoản trước khi phát OTP. */
    @PostMapping("/send-register-otp")
    public ResponseEntity<SendOtpResponse> sendRegisterOtp(@Valid @RequestBody SendOtpRequest req) {
        authService.ensureRegistrationIdentifierAvailable(req.identifier());
        String code = otpService.sendOtp(req.identifier());
        return RestResponses.ok(new SendOtpResponse(exposeOtpCode ? code : null, 300));
    }

    /** Public: xac thuc OTP cho dang ky (khong consume). */
    @PostMapping("/verify-register-otp")
    public ResponseEntity<Void> verifyRegisterOtp(@Valid @RequestBody vn.edu.fpt.cares.dto.auth.VerifyOtpRequest req) {
        otpService.markOtpAsVerified(req.identifier(), req.otp());
        return RestResponses.ok(null);
    }

    /** Public: reset mat khau bang OTP -> 200 OK. */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
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

