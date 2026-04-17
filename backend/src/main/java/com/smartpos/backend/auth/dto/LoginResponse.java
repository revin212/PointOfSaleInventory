package com.smartpos.backend.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        AuthUserResponse user
) {}
