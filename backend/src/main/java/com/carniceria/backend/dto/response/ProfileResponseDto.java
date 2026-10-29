package com.carniceria.backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponseDto(
        UUID id,
        String role,
        String firstName,
        String lastName,
        String phone,
        String addressText,
        Double addressLat,
        Double addressLng,
        Instant createdAt
) {
}
