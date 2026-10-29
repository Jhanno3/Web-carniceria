package com.carniceria.backend.model;

import java.math.BigDecimal;

/** Un ítem de pedido: precio y nombre son una foto del momento, nunca cambian con el producto. */
public record OrderItem(
        String productId,
        String productNameSnapshot,
        Unit unit,
        BigDecimal quantity,
        BigDecimal pricePerUnitSnapshot,
        String itemNotes
) {

    public OrderItem {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad tiene que ser mayor a cero.");
        }
    }

    public static OrderItem of(String productId, String productNameSnapshot, Unit unit,
                                BigDecimal quantity, BigDecimal pricePerUnitSnapshot, String itemNotes) {
        return new OrderItem(productId, productNameSnapshot, unit, quantity, pricePerUnitSnapshot, itemNotes);
    }

    public BigDecimal subtotalEstimated() {
        return pricePerUnitSnapshot == null ? null : pricePerUnitSnapshot.multiply(quantity);
    }
}
