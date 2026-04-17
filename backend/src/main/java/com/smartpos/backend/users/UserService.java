package com.smartpos.backend.users;

import com.smartpos.backend.audit.AuditAction;
import com.smartpos.backend.audit.AuditEntityType;
import com.smartpos.backend.audit.AuditService;
import com.smartpos.backend.auth.RefreshTokenRepository;
import com.smartpos.backend.common.error.ConflictException;
import com.smartpos.backend.common.error.NotFoundException;
import com.smartpos.backend.users.dto.CreateUserRequest;
import com.smartpos.backend.users.dto.SetActiveRequest;
import com.smartpos.backend.users.dto.UpdateUserRequest;
import com.smartpos.backend.users.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String query, Pageable pageable) {
        String q = (query == null || query.isBlank()) ? "" : query.trim().toLowerCase();
        return userRepository.search(q, pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("Email already in use");
        }
        UserEntity user = new UserEntity();
        user.setName(req.name().trim());
        user.setEmail(req.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role());
        user.setActive(req.active() == null ? true : req.active());
        UserEntity saved = userRepository.save(user);
        auditService.record(AuditAction.USER_CREATE, AuditEntityType.USER, saved.getId(),
                Map.of("email", saved.getEmail(), "role", saved.getRole().name()));
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest req) {
        UserEntity user = findOrThrow(id);
        String newEmail = req.email().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new ConflictException("Email already in use");
        }
        user.setName(req.name().trim());
        user.setEmail(newEmail);
        user.setRole(req.role());
        boolean passwordChanged = req.password() != null && !req.password().isBlank();
        if (passwordChanged) {
            user.setPasswordHash(passwordEncoder.encode(req.password()));
            refreshTokenRepository.revokeAllForUser(user.getId());
        }
        UserEntity saved = userRepository.save(user);
        auditService.record(AuditAction.USER_UPDATE, AuditEntityType.USER, saved.getId(),
                Map.of("email", saved.getEmail(), "role", saved.getRole().name(),
                        "passwordChanged", passwordChanged));
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse setActive(UUID id, SetActiveRequest req) {
        UserEntity user = findOrThrow(id);
        boolean newActive = req.active();
        if (user.isActive() && !newActive) {
            refreshTokenRepository.revokeAllForUser(user.getId());
        }
        user.setActive(newActive);
        UserEntity saved = userRepository.save(user);
        auditService.record(AuditAction.USER_SET_ACTIVE, AuditEntityType.USER, saved.getId(),
                Map.of("active", newActive));
        return UserResponse.from(saved);
    }

    private UserEntity findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
