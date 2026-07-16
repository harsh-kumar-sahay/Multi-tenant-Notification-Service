package com.notifsvc.auth;

import com.notifsvc.user.AppUser;
import com.notifsvc.user.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * Built either from the DB (login flow, via CustomUserDetailsService) or directly from
 * JWT claims (JwtAuthFilter, on every subsequent request) - the latter avoids a DB round
 * trip per request at the cost of role/enabled changes only taking effect after the
 * token expires. Acceptable trade-off given this service's scope and token lifetime.
 */
public class JwtUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final Long tenantId;
    private final boolean enabled;

    public JwtUserDetails(Long userId, String username, String passwordHash, Role role, Long tenantId, boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.tenantId = tenantId;
        this.enabled = enabled;
    }

    public static JwtUserDetails fromAppUser(AppUser user) {
        return new JwtUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRole(),
                user.getTenant() != null ? user.getTenant().getId() : null,
                user.isEnabled());
    }

    public Long getUserId() { return userId; }
    public Role getRole() { return role; }
    public Long getTenantId() { return tenantId; }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}
