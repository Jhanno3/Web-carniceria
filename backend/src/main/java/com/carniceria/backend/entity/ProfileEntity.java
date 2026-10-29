package com.carniceria.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Reflejo 1:1 de la tabla `profiles` (ver supabase/migrations/0001_profiles.sql).
 * Sin reglas de negocio: eso vive en model.Profile.
 */
@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class ProfileEntity {

    @Id
    private UUID id;

    private String role;

    private String firstName;

    private String lastName;

    private String phone;

    private String addressText;

    private Double addressLat;

    private Double addressLng;

    private Instant createdAt;
}
