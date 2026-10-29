package com.carniceria.backend.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Reemplaza el JwtDecoder real (que validaría contra el JWKS de Supabase) por uno que confía
 * en la clave pública de TestJwtSupport. Al existir este bean, el auto-configurado por la
 * propiedad spring.security.oauth2.resourceserver.jwt.jwk-set-uri queda de lado.
 */
@TestConfiguration
public class TestJwtConfiguration {

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(TestJwtSupport.publicKey()).build();
    }
}
