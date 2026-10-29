package com.carniceria.backend.dto.response;

import java.math.BigDecimal;

public record OrderItemResponseDto(
        String productId,
        String productName,
        String unit,
        BigDecimal quantity,
        BigDecimal pricePerUnit,
        BigDecimal subtotal,
        String itemNotes
) {
}
