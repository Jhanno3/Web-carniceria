package com.carniceria.backend.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Par de claves de prueba + emisión de JWTs firmados localmente, para no depender del JWKS
 * real de Supabase en los tests end-to-end (ver plan.md de specs/001-autenticacion-y-perfil).
 */
public final class TestJwtSupport {

    public static final KeyPair KEY_PAIR = generateKeyPair();

    private TestJwtSupport() {
    }

    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEY_PAIR.getPublic();
    }

    public static String signedJwtFor(UUID subject) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject.toString())
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .build();
            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            signedJwt.sign(new RSASSASigner((RSAPrivateKey) KEY_PAIR.getPrivate()));
            return signedJwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo firmar el JWT de test", e);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
