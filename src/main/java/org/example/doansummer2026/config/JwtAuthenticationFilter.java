package org.example.doansummer2026.config;

import tools.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.ApiError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.model.Account;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Filter đọc Authorization: Bearer ... , verify JWT và set SecurityContext.
 * Khi token lỗi -> trả về 401 JSON thay vì mặc định của Spring.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final AccountRepository accountRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        try {
            Claims claims = jwtService.parseClaims(token);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            String sid = claims.get("sid", String.class);
            String type = claims.get("type", String.class);

            if (username == null || role == null || !"access".equals(type)) {
                writeError(response, "Token khong hop le", request.getRequestURI());
                return;
            }

            Account account = accountRepository.findFirstByUsername(username).orElse(null);
            if (account == null || !Boolean.TRUE.equals(account.getIsActive())) {
                writeError(response, "Tai khoan khong ton tai hoac da bi khoa", request.getRequestURI());
                return;
            }

            // Authentication info: username + staffId (nếu có)
            Map<String, Object> principal = new HashMap<>();
            principal.put("username", username);
            if (sid != null) {
                principal.put("staffId", sid);
            }

            // Extract authorities from JWT claims (based on SystemRole)
            Collection<String> authorities = claims.get("authorities", Collection.class);
            List<SimpleGrantedAuthority> grantedAuthorities;
            if (authorities != null && !authorities.isEmpty()) {
                grantedAuthorities = authorities.stream()
                        .map(a -> {
                            String authStr = (String) a;
                            // Already has ROLE_ prefix
                            return new SimpleGrantedAuthority(authStr);
                        })
                        .toList();
            } else {
                // Fallback: use ROLE_<ROLE> for backward compatibility
                grantedAuthorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            grantedAuthorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            writeError(response, "Token khong hop le hoac het han", request.getRequestURI());
        }
    }

    private void writeError(HttpServletResponse response, String message, String path) throws IOException {
        ApiError body = new ApiError(
                Instant.now(), 401, "Unauthorized", message, path, null);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

