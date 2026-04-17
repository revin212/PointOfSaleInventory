package com.smartpos.backend.auth;

import com.smartpos.backend.auth.dto.AuthUserResponse;
import com.smartpos.backend.auth.dto.LoginRequest;
import com.smartpos.backend.auth.dto.LoginResponse;
import com.smartpos.backend.auth.dto.RefreshResponse;
import com.smartpos.backend.common.error.ApiException;
import com.smartpos.backend.common.error.ErrorCode;
import com.smartpos.backend.security.JwtService;
import com.smartpos.backend.users.UserEntity;
import com.smartpos.backend.users.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        UserEntity user = userRepository.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Invalid credentials"));
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Account is deactivated");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Invalid credentials");
        }

        String access = jwtService.issueAccessToken(user.getId(), user.getEmail(), user.getName(), user.getRole());
        JwtService.IssuedToken refresh = jwtService.issueRefreshToken(user.getId());
        persistRefresh(user.getId(), refresh);

        return new LoginResponse(
                access,
                refresh.token(),
                jwtService.accessTtlSeconds(),
                new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole())
        );
    }

    @Transactional
    public RefreshResponse refresh(String refreshToken) {
        Claims claims = jwtService.parse(refreshToken)
                .filter(jwtService::isRefreshToken)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Invalid refresh token"));

        String hash = sha256(refreshToken);
        RefreshTokenEntity stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Refresh token not recognized"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Refresh token expired or revoked");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "User not found"));
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Account is deactivated");
        }

        // Rotate: revoke previous, issue new
        stored.setRevoked(true);
        JwtService.IssuedToken newRefresh = jwtService.issueRefreshToken(user.getId());
        RefreshTokenEntity persisted = persistRefresh(user.getId(), newRefresh);
        stored.setReplacedBy(persisted.getId());

        String access = jwtService.issueAccessToken(user.getId(), user.getEmail(), user.getName(), user.getRole());
        return new RefreshResponse(access, newRefresh.token(), jwtService.accessTtlSeconds());
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String hash = sha256(refreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> t.setRevoked(true));
    }

    private RefreshTokenEntity persistRefresh(UUID userId, JwtService.IssuedToken issued) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(userId);
        entity.setTokenHash(sha256(issued.token()));
        entity.setIssuedAt(Instant.now());
        entity.setExpiresAt(issued.expiresAt());
        return refreshTokenRepository.save(entity);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
