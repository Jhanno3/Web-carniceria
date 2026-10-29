package com.carniceria.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Sin precio ni nombre: el backend los busca en `products`, nunca confía en lo que mande el cliente. */
public record OrderItemRequestDto(
        @NotBlank String productId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal quantity,
        String itemNotes
) {
}
