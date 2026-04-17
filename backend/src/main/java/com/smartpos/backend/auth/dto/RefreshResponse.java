package com.smartpos.backend.auth.dto;

public record RefreshResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {}
