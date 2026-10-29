package com.carniceria.backend.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Payment(
        UUID id,
        UUID orderId,
        PaymentMethod method,
        PaymentStatus status,
        String mercadopagoPaymentId,
        String mercadopagoPreferenceId,
        Instant transferMarkedByClientAt,
        BigDecimal amount,
        Instant createdAt,
        Instant confirmedAt
) {
    public Payment {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto del pago no puede ser negativo.");
        }
    }
}
