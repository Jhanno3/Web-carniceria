package com.carniceria.backend.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Nunca tiene un campo "role": el rol no se acepta desde ningún DTO de entrada
 * (ver constitution.md sección 7 y plan.md de specs/001-autenticacion-y-perfil).
 */
public record UpdateProfileRequestDto(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Pattern(regexp = "^[0-9+()\\-\\s]{6,20}$", message = "Formato de teléfono inválido") String phone,
        @Size(max = 300) String addressText,
        Double addressLat,
        Double addressLng
) {
}
