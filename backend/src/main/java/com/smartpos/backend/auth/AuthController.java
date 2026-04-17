package com.smartpos.backend.auth;

import com.smartpos.backend.auth.dto.LoginRequest;
import com.smartpos.backend.auth.dto.LoginResponse;
import com.smartpos.backend.auth.dto.LogoutRequest;
import com.smartpos.backend.auth.dto.MeResponse;
import com.smartpos.backend.auth.dto.RefreshRequest;
import com.smartpos.backend.auth.dto.RefreshResponse;
import com.smartpos.backend.auth.dto.SimpleResponse;
import com.smartpos.backend.security.AppUserPrincipal;
import com.smartpos.backend.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public SimpleResponse logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return SimpleResponse.ok();
    }

    @GetMapping("/me")
    public MeResponse me() {
        AppUserPrincipal p = SecurityUtils.requirePrincipal();
        return new MeResponse(p.getId(), p.getName(), p.getEmail(), p.getRole(), true);
    }
}
