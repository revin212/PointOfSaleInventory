package com.smartpos.backend.security;

import com.smartpos.backend.domain.enums.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length()).trim();
            jwtService.parse(token).ifPresent(claims -> authenticateIfAccess(claims, request));
        }
        chain.doFilter(request, response);
    }

    private void authenticateIfAccess(Claims claims, HttpServletRequest request) {
        if (!jwtService.isAccessToken(claims)) {
            return;
        }
        try {
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get(JwtService.CLAIM_EMAIL, String.class);
            String name = claims.get(JwtService.CLAIM_NAME, String.class);
            Role role = Role.valueOf(claims.get(JwtService.CLAIM_ROLE, String.class));

            AppUserPrincipal principal = new AppUserPrincipal(userId, email, "", role, true, name);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (IllegalArgumentException ignored) {
            // bad subject/role; leave context unauthenticated
        }
    }
}
