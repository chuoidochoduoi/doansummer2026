package org.example.doansummer2026.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtService jwt = mock(JwtService.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            jwt, new tools.jackson.databind.ObjectMapper(), accounts);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestWithoutTokenContinuesUnauthenticated() throws Exception {
        var request = new MockHttpServletRequest("GET", "/public");
        var response = new MockHttpServletResponse();
        var chain = mock(jakarta.servlet.FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void validBearerAuthenticatesWithAuthoritiesAndStaffId() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/me");
        request.addHeader("Authorization", "Bearer valid");
        var response = new MockHttpServletResponse();
        var chain = mock(jakarta.servlet.FilterChain.class);
        Claims claims = claims("doctor", "DOCTOR", "access", "staff-1", List.of("ROLE_DOCTOR", "READ"));
        when(jwt.parseClaims("valid")).thenReturn(claims);
        when(accounts.findFirstByUsername("doctor")).thenReturn(Optional.of(active(true)));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("staff-1", ((Map<?, ?>) authentication.getPrincipal()).get("staffId"));
        assertEquals(2, authentication.getAuthorities().size());
    }

    @Test
    void queryTokenUsesRoleFallbackWithoutStaffId() throws Exception {
        var request = new MockHttpServletRequest("GET", "/files/result.pdf");
        request.setParameter("token", "query-token");
        var response = new MockHttpServletResponse();
        var chain = mock(jakarta.servlet.FilterChain.class);
        Claims queryClaims = claims("customer", "CUSTOMER", "access", null, List.of());
        when(jwt.parseClaims("query-token")).thenReturn(queryClaims);
        when(accounts.findFirstByUsername("customer")).thenReturn(Optional.of(active(true)));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(List.of("ROLE_CUSTOMER"), authentication.getAuthorities().stream()
                .map(Object::toString).toList());
        assertFalse(((Map<?, ?>) authentication.getPrincipal()).containsKey("staffId"));
    }

    @Test
    void blankQueryTokenIsIgnored() throws Exception {
        var request = new MockHttpServletRequest("GET", "/files/result.pdf");
        request.setParameter("token", "  ");
        var response = new MockHttpServletResponse();
        var chain = mock(jakarta.servlet.FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwt);
    }

    @Test
    void malformedClaimsReturnJsonUnauthorized() throws Exception {
        for (Claims claims : List.of(
                claims(null, "DOCTOR", "access", null, null),
                claims("u", null, "access", null, null),
                claims("u", "DOCTOR", "refresh", null, null))) {
            var request = new MockHttpServletRequest("GET", "/api/private");
            request.addHeader("Authorization", "Bearer bad-claims");
            var response = new MockHttpServletResponse();
            var chain = mock(jakarta.servlet.FilterChain.class);
            when(jwt.parseClaims("bad-claims")).thenReturn(claims);
            filter.doFilter(request, response, chain);
            assertEquals(401, response.getStatus());
            assertTrue(response.getContentAsString().contains("Token không hợp lệ"));
            verify(chain, never()).doFilter(request, response);
            reset(jwt);
        }
    }

    @Test
    void missingOrLockedAccountReturnsUnauthorized() throws Exception {
        for (Optional<Account> account : List.of(Optional.<Account>empty(), Optional.of(active(false)))) {
            var request = new MockHttpServletRequest("GET", "/api/private");
            request.addHeader("Authorization", "Bearer valid");
            var response = new MockHttpServletResponse();
            var chain = mock(jakarta.servlet.FilterChain.class);
            Claims validClaims = claims("u", "DOCTOR", "access", null, null);
            when(jwt.parseClaims("valid")).thenReturn(validClaims);
            when(accounts.findFirstByUsername("u")).thenReturn(account);
            filter.doFilter(request, response, chain);
            assertEquals(401, response.getStatus());
            assertTrue(response.getContentAsString().contains("đã bị khóa"));
            verify(chain, never()).doFilter(request, response);
            reset(jwt, accounts);
        }
    }

    @Test
    void invalidJwtReturnsUnauthorizedWithoutCallingChain() throws Exception {
        var request = new MockHttpServletRequest("GET", "/secure");
        request.addHeader("Authorization", "Bearer expired");
        var response = new MockHttpServletResponse();
        var chain = mock(jakarta.servlet.FilterChain.class);
        when(jwt.parseClaims("expired")).thenThrow(new JwtException("expired"));

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("hết hạn"));
        verify(chain, never()).doFilter(request, response);
    }

    private Claims claims(String subject, String role, String type, String sid, List<String> authorities) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(subject);
        when(claims.get("role", String.class)).thenReturn(role);
        when(claims.get("type", String.class)).thenReturn(type);
        when(claims.get("sid", String.class)).thenReturn(sid);
        when(claims.get("authorities", java.util.Collection.class)).thenReturn(authorities);
        return claims;
    }

    private Account active(boolean active) {
        return Account.builder().isActive(active).build();
    }
}
