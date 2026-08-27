package org.example.doansummer2026.dto.auth;

import org.example.doansummer2026.enums.SystemRole;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        AccountInfo account
) {
    public record AccountInfo(
            UUID accountId,
            String username,
            String role,
            String systemRole
    ) {
        public static AccountInfo from(AccountInfo info) {
            return info;
        }
    }
}



