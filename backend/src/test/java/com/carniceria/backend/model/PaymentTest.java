package com.carniceria.backend.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void rechaza_un_monto_negativo() {
        assertThatThrownBy(() -> new Payment(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.EFECTIVO,
                PaymentStatus.PENDIENTE, null, null, null, new BigDecimal("-1"), Instant.now(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acepta_monto_cero_mientras_haya_productos_sin_precio_cargado_todavia() {
        // No debe lanzar: el catálogo de almacén todavía tiene precios placeholder (null).
        new Payment(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.EFECTIVO,
                PaymentStatus.PENDIENTE, null, null, null, BigDecimal.ZERO, Instant.now(), null);
    }
}
