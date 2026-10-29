package com.carniceria.backend.security;

import com.carniceria.backend.model.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public static Role role(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin ? Role.ADMIN : Role.CLIENT;
    }
}
