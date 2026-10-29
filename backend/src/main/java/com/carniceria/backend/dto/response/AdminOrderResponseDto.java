package com.carniceria.backend.dto.response;

public record AdminOrderResponseDto(
        OrderResponseDto order,
        String clientFirstName,
        String clientLastName,
        String clientPhone
) {
}
