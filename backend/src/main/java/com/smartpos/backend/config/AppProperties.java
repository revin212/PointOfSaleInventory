package com.smartpos.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String basePath,
        Cors cors,
        Jwt jwt,
        Seed seed
) {
    public record Cors(String allowedOrigins) {}

    public record Jwt(
            String issuer,
            String secret,
            long accessTokenTtlSeconds,
            long refreshTokenTtlSeconds
    ) {}

    public record Seed(boolean enabled, String defaultPassword) {}
}
