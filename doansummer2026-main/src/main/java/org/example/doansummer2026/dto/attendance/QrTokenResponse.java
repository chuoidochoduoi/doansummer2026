package org.example.doansummer2026.dto.attendance;
import java.time.LocalDateTime;
public record QrTokenResponse(String value, LocalDateTime expiresAt, long expiresInSeconds) {}
