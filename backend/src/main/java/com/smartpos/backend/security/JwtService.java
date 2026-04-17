package com.smartpos.backend.security;

import com.smartpos.backend.config.AppProperties;
import com.smartpos.backend.domain.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    public static final String CLAIM_TOKEN_TYPE = "type";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_NAME = "name";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final AppProperties props;
    private final SecretKey signingKey;

    public JwtService(AppProperties props) {
        this.props = props;
        byte[] keyBytes = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes (HMAC-SHA256)");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String issueAccessToken(UUID userId, String email, String name, Role role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.jwt().accessTokenTtlSeconds());
        return Jwts.builder()
                .issuer(props.jwt().issuer())
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_NAME, name)
                .signWith(signingKey)
                .compact();
    }

    public IssuedToken issueRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.jwt().refreshTokenTtlSeconds());
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .issuer(props.jwt().issuer())
                .subject(userId.toString())
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim(CLAIM_TOKEN_TYPE, TYPE_REFRESH)
                .signWith(signingKey)
                .compact();
        return new IssuedToken(token, jti, exp);
    }

    public long accessTtlSeconds() {
        return props.jwt().accessTokenTtlSeconds();
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public boolean isAccessToken(Claims c) {
        return TYPE_ACCESS.equals(c.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims c) {
        return TYPE_REFRESH.equals(c.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public record IssuedToken(String token, String jti, Instant expiresAt) {}
}
