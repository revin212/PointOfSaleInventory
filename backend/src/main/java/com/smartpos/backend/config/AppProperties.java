package com.smartpos.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String basePath,
        Cors cors,
        Jwt jwt,
        Tax tax,
        Shift shift,
        Seed seed
) {
    public record Cors(String allowedOrigins) {}

    public record Jwt(
            String issuer,
            String secret,
            long accessTokenTtlSeconds,
            long refreshTokenTtlSeconds
    ) {}

    public record Tax(
            boolean enabled,
            String mode,
            java.math.BigDecimal vatRate
    ) {}

    public record Shift(
            boolean requireOpenForCashSales
    ) {}

    public record Seed(boolean enabled, String defaultPassword, boolean forceResetPasswords) {}
}
