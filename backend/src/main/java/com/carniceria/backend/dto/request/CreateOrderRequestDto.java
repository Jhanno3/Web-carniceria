package com.carniceria.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequestDto(
        @NotBlank String paymentMethod,
        @NotBlank String deliveryType,
        String addressText,
        Double addressLat,
        Double addressLng,
        String notes,
        @NotEmpty List<@Valid OrderItemRequestDto> items
) {
}
