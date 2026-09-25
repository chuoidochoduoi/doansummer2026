package vn.edu.fpt.cares.config;

import io.jsonwebtoken.Claims;
import vn.edu.fpt.cares.enums.*;
import vn.edu.fpt.cares.model.Account;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = Base64.getEncoder().encodeToString(
            "cares-demo-test-secret-with-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));

    @Test
    void rejectsSecretShorterThanHs256Requirement() {
        String shortSecret = Base64.getEncoder().encodeToString("short".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalStateException.class, () -> new JwtService(shortSecret, 60_000, 120_000));
    }

    @Test
    void customerTokensCarryIdentityTypeExpiryAndCustomerAuthority() {
        JwtService jwt = new JwtService(SECRET, 60_000, 120_000);
        Account account = account(Role.CUSTOMER);
        Claims access = jwt.parseClaims(jwt.generateAccessToken(account));
        Claims refresh = jwt.parseClaims(jwt.generateRefreshToken(account));

        assertEquals(account.getUsername(), access.getSubject());
        assertEquals(account.getAccountId().toString(), access.get("uid", String.class));
        assertEquals("access", access.get("type", String.class));
        assertEquals(List.of("ROLE_CUSTOMER"), access.get("authorities", List.class));
        assertEquals(Role.CUSTOMER, jwt.extractRole(access));
        assertNull(jwt.extractStaffId(access));
        assertEquals("refresh", refresh.get("type", String.class));
        assertTrue(refresh.getExpiration().after(access.getExpiration()));
        assertEquals(60_000, jwt.getAccessExpirationMs());
        assertEquals(120_000, jwt.getRefreshExpirationMs());
    }

    @Test
    void legacyStaffWithoutSystemRoleGetsGenericAuthorityAndStaffIdOverloadsWork() {
        JwtService jwt = new JwtService(SECRET, 60_000, 120_000);
        Account staff = account(Role.STAFF);
        UUID staffId = UUID.randomUUID();
        Claims noRole = jwt.parseClaims(jwt.generateAccessToken(staff));
        Claims access = jwt.parseClaims(jwt.generateAccessToken(staff, staffId));
        Claims refresh = jwt.parseClaims(jwt.generateRefreshToken(staff, staffId));
        assertEquals(List.of("ROLE_STAFF"), noRole.get("authorities", List.class));
        assertEquals(staffId, jwt.extractStaffId(access));
        assertEquals(staffId, jwt.extractStaffId(refresh));
    }

    @Test
    void everySystemRoleMapsToExpectedAuthoritiesForAccessAndRefresh() {
        JwtService jwt = new JwtService(SECRET, 60_000, 120_000);
        Account staff = account(Role.STAFF);
        UUID staffId = UUID.randomUUID();
        Map<SystemRole, String> expected = new LinkedHashMap<>();
        expected.put(SystemRole.DOCTOR, "ROLE_DOCTOR");
        expected.put(SystemRole.GENERAL_DOCTOR, "ROLE_DOCTOR");
        expected.put(SystemRole.SPECIALIST_DOCTOR, "ROLE_DOCTOR");
        expected.put(SystemRole.NURSE, "ROLE_NURSE");
        expected.put(SystemRole.RECEPTIONIST, "ROLE_RECEPTIONIST");
        expected.put(SystemRole.CASHIER, "ROLE_CASHIER");
        expected.put(SystemRole.CLINIC_MANAGER, "ROLE_CLINIC_MANAGER");
        expected.put(SystemRole.ADMIN, "ROLE_ADMIN");

        expected.forEach((role, authority) -> {
            Claims access = jwt.parseClaims(jwt.generateAccessToken(staff, staffId, role));
            Claims refresh = jwt.parseClaims(jwt.generateRefreshToken(staff, staffId, role));
            assertEquals(List.of(authority, "ROLE_STAFF"), access.get("authorities", List.class));
            assertEquals(List.of(authority, "ROLE_STAFF"), refresh.get("authorities", List.class));
            assertEquals(staffId, jwt.extractStaffId(access));
        });
    }

    private Account account(Role role) {
        return Account.builder().accountId(UUID.randomUUID()).username("user-" + role.name().toLowerCase())
                .role(role).isActive(true).build();
    }
}
