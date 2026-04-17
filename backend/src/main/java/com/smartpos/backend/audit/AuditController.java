package com.smartpos.backend.audit;

import com.smartpos.backend.audit.dto.AuditLogResponse;
import com.smartpos.backend.common.web.PageResponse;
import com.smartpos.backend.common.web.PageableSupport;
import com.smartpos.backend.users.UserEntity;
import com.smartpos.backend.users.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditController {

    private final AuditLogRepository repository;
    private final UserRepository userRepository;

    public AuditController(AuditLogRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        Instant fromI = from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toI   = to   == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Page<AuditLogEntity> result = repository.search(entityType, action, userId, fromI, toI,
                PageableSupport.resolve(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        Set<UUID> userIds = result.getContent().stream()
                .map(AuditLogEntity::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> names = new HashMap<>();
        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds).forEach(u -> names.put(u.getId(), u.getName()));
        }
        return PageResponse.from(result.map(a ->
                AuditLogResponse.from(a, a.getUserId() == null ? null : names.get(a.getUserId()))));
    }
}
