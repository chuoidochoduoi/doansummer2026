package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.dto.auth.AuthResponse;
import vn.edu.fpt.cares.dto.auth.ChangePasswordRequest;
import vn.edu.fpt.cares.dto.auth.LoginRequest;
import vn.edu.fpt.cares.dto.auth.RefreshRequest;
import vn.edu.fpt.cares.dto.auth.RegisterRequest;
import vn.edu.fpt.cares.enums.SystemRole;
import vn.edu.fpt.cares.model.Account;

public interface AuthServiceInterface {
    AuthResponse register(RegisterRequest req);
    AuthResponse login(LoginRequest req);
    AuthResponse refresh(RefreshRequest req);
    Account currentAccount();
    SystemRole getCurrentSystemRole();
    void changeMyPassword(ChangePasswordRequest req);
    void resetPassword(vn.edu.fpt.cares.dto.auth.ResetPasswordRequest req);
    java.util.Map<String, Boolean> registrationAvailability(String identifier);
    void ensureRegistrationIdentifierAvailable(String identifier);
}


