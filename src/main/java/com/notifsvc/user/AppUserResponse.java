package com.notifsvc.user;

public record AppUserResponse(Long id, String username, Role role, Long tenantId, boolean enabled) {
    public static AppUserResponse from(AppUser user) {
        return new AppUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getTenant() != null ? user.getTenant().getId() : null,
                user.isEnabled());
    }
}
