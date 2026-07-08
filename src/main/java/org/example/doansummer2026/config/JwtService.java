package org.example.doansummer2026.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Sinh và xác thực JWT (access + refresh).
 * - Secret lưu dạng base64 trong application.properties, giải mã thành byte[] để tạo HMAC-SHA key.
 * - Cần tối thiểu 256-bit (32 bytes) cho thuật toán HS256.
 */
@Component
public class JwtService {

    private final SecretKey key;
    @Getter
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expiration-ms}") long accessExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret phai giai ma ra it nhat 32 bytes (256-bit) cho HMAC-SHA256");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(Account account) {
        return buildToken(account, accessExpirationMs, "access");
    }

    public String generateRefreshToken(Account account) {
        return buildToken(account, refreshExpirationMs, "refresh");
    }

    private String buildToken(Account account, long expirationMs, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(account.getUsername())
                .claim("uid", account.getAccountId().toString())
                .claim("role", account.getRole().name())
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public Role extractRole(Claims claims) {
        return Role.valueOf(claims.get("role", String.class));
    }
}