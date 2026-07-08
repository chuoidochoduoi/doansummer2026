package org.example.doansummer2026.dto.auth;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        AccountInfo account
) {
    public record AccountInfo(UUID accountId, String username, String role) {}
}