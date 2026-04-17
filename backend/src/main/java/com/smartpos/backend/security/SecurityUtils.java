package com.smartpos.backend.security;

import com.smartpos.backend.common.error.ErrorCode;
import com.smartpos.backend.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<AppUserPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AppUserPrincipal p)) {
            return Optional.empty();
        }
        return Optional.of(p);
    }

    public static AppUserPrincipal requirePrincipal() {
        return currentPrincipal().orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Authentication required"));
    }

    public static UUID currentUserId() {
        return requirePrincipal().getId();
    }

    public static UUID currentUserIdOrNull() {
        return currentPrincipal().map(AppUserPrincipal::getId).orElse(null);
    }
}
