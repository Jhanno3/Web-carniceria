package com.carniceria.backend.security;

import com.carniceria.backend.repository.ProfileRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Convierte el JWT ya validado (firma/expiración las valida Spring Security contra el JWKS de Supabase)
 * en una Authentication cuyo rol viene SIEMPRE de la tabla `profiles`, nunca de un claim del propio
 * token. Así ningún cliente puede auto-asignarse `admin` manipulando el JWT (ver constitution.md
 * sección 7: "nunca confía en un claim de rol que venga del cliente").
 */
@Component
public class ProfileAuthoritiesJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final ProfileRepository profileRepository;

    public ProfileAuthoritiesJwtAuthenticationConverter(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String role = profileRepository.findById(userId)
                .map(p -> p.getRole().toUpperCase())
                .orElse("CLIENT");

        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        return new JwtAuthenticationToken(jwt, authorities, userId.toString());
    }
}
