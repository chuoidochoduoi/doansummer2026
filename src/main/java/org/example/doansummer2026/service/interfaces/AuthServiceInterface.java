package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.dto.auth.AuthResponse;
import org.example.doansummer2026.dto.auth.ChangePasswordRequest;
import org.example.doansummer2026.dto.auth.LoginRequest;
import org.example.doansummer2026.dto.auth.RefreshRequest;
import org.example.doansummer2026.dto.auth.RegisterRequest;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.model.Account;

public interface AuthServiceInterface {
    AuthResponse register(RegisterRequest req);
    AuthResponse login(LoginRequest req);
    AuthResponse refresh(RefreshRequest req);
    Account currentAccount();
    SystemRole getCurrentSystemRole();
    void changeMyPassword(ChangePasswordRequest req);
    void resetPassword(org.example.doansummer2026.dto.auth.ResetPasswordRequest req);
    java.util.Map<String, Boolean> registrationAvailability(String identifier);
    void ensureRegistrationIdentifierAvailable(String identifier);
}


