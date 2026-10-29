package com.carniceria.backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponseDto(
        UUID id,
        String status,
        String paymentMethod,
        String paymentStatus,
        String deliveryType,
        String addressText,
        Double addressLat,
        Double addressLng,
        BigDecimal totalEstimated,
        BigDecimal shippingCost,
        String notes,
        List<OrderItemResponseDto> items,
        Instant createdAt,
        String mercadopagoInitPoint
) {
}
