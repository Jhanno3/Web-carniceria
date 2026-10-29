package com.carniceria.backend.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Reglas de negocio del pedido. El total NUNCA se acepta de afuera: el constructor compacto
 * lo recalcula siempre a partir de los ítems, así que no hay forma de construir un Order con
 * un total que no coincida con la suma real (ver constitution.md sección 7).
 */
public record Order(
        UUID id,
        UUID userId,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        DeliveryType deliveryType,
        String deliveryAddressText,
        Double deliveryAddressLat,
        Double deliveryAddressLng,
        BigDecimal totalEstimated,
        String notes,
        List<OrderItem> items,
        Instant createdAt,
        Instant updatedAt
) {

    /** Costo de envío a domicilio; gratis a partir de este subtotal (ver constitution.md sección 4). */
    public static final BigDecimal SHIPPING_FEE = new BigDecimal("2500");
    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("40000");

    public Order {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El pedido necesita al menos un ítem.");
        }
        items = List.copyOf(items);
        totalEstimated = subtotal(items).add(shippingCost(items, deliveryType));
    }

    private static BigDecimal subtotal(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::subtotalEstimated)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal shippingCost(List<OrderItem> items, DeliveryType deliveryType) {
        if (deliveryType != DeliveryType.ENVIO) {
            return BigDecimal.ZERO;
        }
        return subtotal(items).compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : SHIPPING_FEE;
    }

    /** Siempre recalculado de los ítems y el tipo de entrega, igual que el total (nunca se acepta de afuera). */
    public BigDecimal shippingCost() {
        return shippingCost(items, deliveryType);
    }

    /** Efectivo queda confirmado de entrada (se cobra al entregar); transferencia espera el pago. */
    public static Order create(UUID id, UUID userId, PaymentMethod paymentMethod, DeliveryType deliveryType,
                                String deliveryAddressText, Double deliveryAddressLat, Double deliveryAddressLng,
                                String notes, List<OrderItem> items, Instant now) {
        OrderStatus initialStatus = paymentMethod == PaymentMethod.EFECTIVO
                ? OrderStatus.CONFIRMADO
                : OrderStatus.PENDIENTE_PAGO;
        return new Order(id, userId, initialStatus, paymentMethod, PaymentStatus.PENDIENTE, deliveryType,
                deliveryAddressText, deliveryAddressLat, deliveryAddressLng, null, notes, items, now, now);
    }

    public Order markTransferSent() {
        if (paymentMethod != PaymentMethod.TRANSFERENCIA || status != OrderStatus.PENDIENTE_PAGO) {
            throw new IllegalStateException("Solo se puede marcar \"ya transferí\" en un pedido por transferencia pendiente de pago.");
        }
        return withStatus(OrderStatus.PAGO_EN_REVISION, paymentStatus);
    }

    public Order confirmTransferPayment() {
        if (paymentMethod != PaymentMethod.TRANSFERENCIA || status != OrderStatus.PAGO_EN_REVISION) {
            throw new IllegalStateException("Solo se puede confirmar una transferencia que ya fue marcada como enviada.");
        }
        return withStatus(OrderStatus.CONFIRMADO, PaymentStatus.COMPLETADO);
    }

    /** Idempotente: Mercado Pago puede mandar el mismo webhook más de una vez. */
    public Order confirmMercadoPagoPayment() {
        requireMercadoPago();
        if (status == OrderStatus.CONFIRMADO && paymentStatus == PaymentStatus.COMPLETADO) {
            return this;
        }
        return withStatus(OrderStatus.CONFIRMADO, PaymentStatus.COMPLETADO);
    }

    public Order rejectMercadoPagoPayment() {
        requireMercadoPago();
        return withStatus(status, PaymentStatus.RECHAZADO);
    }

    private void requireMercadoPago() {
        if (paymentMethod != PaymentMethod.MERCADOPAGO) {
            throw new IllegalStateException("Este pedido no es de Mercado Pago.");
        }
    }

    private Order withStatus(OrderStatus newStatus, PaymentStatus newPaymentStatus) {
        return new Order(id, userId, newStatus, paymentMethod, newPaymentStatus, deliveryType,
                deliveryAddressText, deliveryAddressLat, deliveryAddressLng, totalEstimated, notes, items,
                createdAt, Instant.now());
    }
}
