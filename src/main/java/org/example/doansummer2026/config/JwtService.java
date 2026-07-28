package org.example.doansummer2026.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.enums.SystemRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Sinh và xác thực JWT (access + refresh).
 * - Secret lưu dạng base64 trong application.properties, giải mã thành byte[] để tạo HMAC-SHA key.
 * - Cần tối thiểu 256-bit (32 bytes) cho thuật toán HS256.
 * - Authorities dựa trên Role và SystemRole: CUSTOMER -> ROLE_CUSTOMER, staff -> ROLE_xxx dựa trên SystemRole.
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
                    "app.jwt.secret phai giau ma ra it nhat 32 bytes (256-bit) cho HMAC-SHA256");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(Account account) {
        return buildToken(account, accessExpirationMs, "access", null, null);
    }

    public String generateAccessToken(Account account, UUID staffId) {
        return buildToken(account, accessExpirationMs, "access", staffId, null);
    }

    public String generateAccessToken(Account account, UUID staffId, SystemRole systemRole) {
        return buildToken(account, accessExpirationMs, "access", staffId, systemRole);
    }

    public String generateRefreshToken(Account account) {
        return buildToken(account, refreshExpirationMs, "refresh", null, null);
    }

    public String generateRefreshToken(Account account, UUID staffId) {
        return buildToken(account, refreshExpirationMs, "refresh", staffId, null);
    }

    public String generateRefreshToken(Account account, UUID staffId, SystemRole systemRole) {
        return buildToken(account, refreshExpirationMs, "refresh", staffId, systemRole);
    }

    private String buildToken(Account account, long expirationMs, String type, UUID staffId, SystemRole systemRole) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(account.getUsername())
                .claim("uid", account.getAccountId().toString())
                .claim("role", account.getRole().name())
                .claim("sid", staffId != null ? staffId.toString() : null)
                .claim("type", type)
                .claim("authorities", authoritiesFromRole(account.getRole(), systemRole))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    private Collection<String> authoritiesFromRole(Role role, SystemRole systemRole) {
        if (role == Role.CUSTOMER) {
            return Collections.singletonList("ROLE_CUSTOMER");
        }
        if (systemRole != null) {
            return switch (systemRole) {
                case GENERAL_DOCTOR, SPECIALIST_DOCTOR -> List.of("ROLE_DOCTOR", "ROLE_STAFF");
                case NURSE -> List.of("ROLE_NURSE", "ROLE_STAFF");
                case RECEPTIONIST -> List.of("ROLE_RECEPTIONIST", "ROLE_STAFF");
                case CASHIER -> List.of("ROLE_CASHIER", "ROLE_STAFF");
                case CLINIC_MANAGER -> List.of("ROLE_CLINIC_MANAGER", "ROLE_STAFF");
                case ADMIN -> List.of("ROLE_ADMIN", "ROLE_STAFF");
            };
        }
        return Collections.singletonList("ROLE_STAFF");
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

    /** Trích xuất staffId từ claims (null nếu không phải staff). */
    public UUID extractStaffId(Claims claims) {
        String sid = claims.get("sid", String.class);
        return sid != null ? UUID.fromString(sid) : null;
    }
}