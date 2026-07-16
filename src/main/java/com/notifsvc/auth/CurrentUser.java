package com.notifsvc.auth;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static JwtUserDetails get() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof JwtUserDetails details) {
            return details;
        }
        throw new UsernameNotFoundException("No authenticated user in security context");
    }
}
