package com.smartpos.backend.security;

import com.smartpos.backend.domain.enums.Role;
import com.smartpos.backend.users.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AppUserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean active;
    private final String name;

    public AppUserPrincipal(UUID id, String email, String passwordHash, Role role, boolean active, String name) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.name = name;
    }

    public static AppUserPrincipal from(UserEntity u) {
        return new AppUserPrincipal(u.getId(), u.getEmail(), u.getPasswordHash(),
                u.getRole(), u.isActive(), u.getName());
    }

    public UUID getId() { return id; }
    public Role getRole() { return role; }
    public String getEmail() { return email; }
    public String getName() { return name; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return active; }
    @Override public boolean isAccountNonLocked() { return active; }
    @Override public boolean isCredentialsNonExpired() { return active; }
    @Override public boolean isEnabled() { return active; }
}
