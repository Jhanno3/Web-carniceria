package com.carniceria.backend.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private OrderItem item(String productId, BigDecimal price, BigDecimal qty) {
        return OrderItem.of(productId, "Nombre " + productId, Unit.KG, qty, price, null);
    }

    @Test
    void item_rechaza_cantidad_cero_o_negativa() {
        assertThatThrownBy(() -> item("asado", new BigDecimal("18500"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void item_calcula_subtotal_como_precio_por_cantidad() {
        OrderItem item = item("asado", new BigDecimal("18500"), new BigDecimal("2"));
        assertThat(item.subtotalEstimated()).isEqualByComparingTo("37000");
    }

    @Test
    void item_sin_precio_tiene_subtotal_nulo() {
        OrderItem item = item("yerba", null, BigDecimal.ONE);
        assertThat(item.subtotalEstimated()).isNull();
    }

    @Test
    void pedido_sin_items_se_rechaza() {
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.EFECTIVO,
                DeliveryType.RETIRO, null, null, null, null, List.of(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void el_total_siempre_se_calcula_de_los_items_nunca_se_acepta_de_afuera() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.EFECTIVO,
                DeliveryType.RETIRO, null, null, null, null,
                List.of(item("asado", new BigDecimal("18500"), new BigDecimal("2")),
                        item("vacio", new BigDecimal("20000"), new BigDecimal("1"))),
                Instant.now());

        assertThat(order.totalEstimated()).isEqualByComparingTo("57000");
    }

    @Test
    void envio_a_domicilio_suma_costo_de_envio_si_no_llega_al_minimo_gratis() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.EFECTIVO,
                DeliveryType.ENVIO, "Calle Falsa 123", null, null, null,
                List.of(item("asado", new BigDecimal("18500"), new BigDecimal("2"))), Instant.now());

        assertThat(order.shippingCost()).isEqualByComparingTo("2500");
        assertThat(order.totalEstimated()).isEqualByComparingTo("39500");
    }

    @Test
    void envio_a_domicilio_es_gratis_desde_el_minimo() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.EFECTIVO,
                DeliveryType.ENVIO, "Calle Falsa 123", null, null, null,
                List.of(item("asado", new BigDecimal("20000"), new BigDecimal("2"))), Instant.now());

        assertThat(order.shippingCost()).isEqualByComparingTo("0");
        assertThat(order.totalEstimated()).isEqualByComparingTo("40000");
    }

    @Test
    void retiro_en_el_local_nunca_tiene_costo_de_envio() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.EFECTIVO,
                DeliveryType.RETIRO, null, null, null, null,
                List.of(item("asado", new BigDecimal("18500"), BigDecimal.ONE)), Instant.now());

        assertThat(order.shippingCost()).isEqualByComparingTo("0");
    }

    @Test
    void efectivo_queda_confirmado_de_entrada() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.EFECTIVO,
                DeliveryType.RETIRO, null, null, null, null,
                List.of(item("asado", new BigDecimal("18500"), BigDecimal.ONE)), Instant.now());

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMADO);
        assertThat(order.paymentStatus()).isEqualTo(PaymentStatus.PENDIENTE);
    }

    @Test
    void transferencia_queda_pendiente_de_pago_de_entrada() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.TRANSFERENCIA,
                DeliveryType.RETIRO, null, null, null, null,
                List.of(item("asado", new BigDecimal("18500"), BigDecimal.ONE)), Instant.now());

        assertThat(order.status()).isEqualTo(OrderStatus.PENDIENTE_PAGO);
    }

    @Test
    void marcar_transferencia_enviada_cambia_a_pago_en_revision() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.TRANSFERENCIA,
                DeliveryType.RETIRO, null, null, null, null,
                List.of(item("asado", new BigDecimal("18500"), BigDecimal.ONE)), Instant.now());

        Order marcado = order.markTransferSent();

        assertThat(marcado.status()).isEqualTo(OrderStatus.PAGO_EN_REVISION);
        assertThat(marcado.paymentStatus()).isEqualTo(PaymentStatus.PENDIENTE);
    }

    @Test
    void no_se_puede_marcar_transferencia_enviada_si_el_metodo_no_es_transferencia() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.EFECTIVO,
                DeliveryType.RETIRO, null, null, null, null,
                List.of(item("asado", new BigDecimal("18500"), BigDecimal.ONE)), Instant.now());

        assertThatThrownBy(order::markTransferSent).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void no_se_puede_marcar_transferencia_enviada_dos_veces() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.TRANSFERENCIA,
                DeliveryType.RETIRO, null, null, null, null,
                List.of(item("asado", new BigDecimal("18500"), BigDecimal.ONE)), Instant.now());

        Order marcado = order.markTransferSent();

        assertThatThrownBy(marcado::markTransferSent).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void confirmar_transferencia_deja_el_pedido_confirmado_y_pagado() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.TRANSFERENCIA,
                DeliveryType.RETIRO, null, null, null, null,
                List.of(item("asado", new BigDecimal("18500"), BigDecimal.ONE)), Instant.now());

        Order confirmado = order.markTransferSent().confirmTransferPayment();

        assertThat(confirmado.status()).isEqualTo(OrderStatus.CONFIRMADO);
        assertThat(confirmado.paymentStatus()).isEqualTo(PaymentStatus.COMPLETADO);
    }

    @Test
    void no_se_puede_confirmar_transferencia_sin_que_la_hayan_marcado_antes() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.TRANSFERENCIA,
                DeliveryType.RETIRO, null, null, null, null,
                List.of(item("asado", new BigDecimal("18500"), BigDecimal.ONE)), Instant.now());

        assertThatThrownBy(order::confirmTransferPayment).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mercadopago_queda_pendiente_de_pago_de_entrada() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.MERCADOPAGO,
                DeliveryType.RETIRO, null, null, null, null,
                List.of(item("asado", new BigDecimal("18500"), BigDecimal.ONE)), Instant.now());

        assertThat(order.status()).isEqualTo(OrderStatus.PENDIENTE_PAGO);
    }

    @Test
    void confirmar_pago_de_mercadopago_deja_el_pedido_confirmado_y_pagado() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.MERCADOPAGO,
                DeliveryType.RETIRO, null, null, null, null,
                List.of(item("asado", new BigDecimal("18500"), BigDecimal.ONE)), Instant.now());

        Order confirmado = order.confirmMercadoPagoPayment();

        assertThat(confirmado.status()).isEqualTo(OrderStatus.CONFIRMADO);
        assertThat(confirmado.paymentStatus()).isEqualTo(PaymentStatus.COMPLETADO);
    }

    @Test
    void confirmar_pago_de_mercadopago_es_idempotente() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.MERCADOPAGO,
                DeliveryType.RETIRO, null, null, null, null,
                List.of(item("asado", new BigDecimal("18500"), BigDecimal.ONE)), Instant.now());

        Order confirmadoUnaVez = order.confirmMercadoPagoPayment();
        Order confirmadoDeNuevo = confirmadoUnaVez.confirmMercadoPagoPayment();

        assertThat(confirmadoDeNuevo.status()).isEqualTo(OrderStatus.CONFIRMADO);
        assertThat(confirmadoDeNuevo.paymentStatus()).isEqualTo(PaymentStatus.COMPLETADO);
    }

    @Test
    void rechazar_pago_de_mercadopago_marca_el_pago_como_rechazado() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.MERCADOPAGO,
                DeliveryType.RETIRO, null, null, null, null,
                List.of(item("asado", new BigDecimal("18500"), BigDecimal.ONE)), Instant.now());

        Order rechazado = order.rejectMercadoPagoPayment();

        assertThat(rechazado.paymentStatus()).isEqualTo(PaymentStatus.RECHAZADO);
    }

    @Test
    void no_se_puede_confirmar_pago_de_mercadopago_en_un_pedido_de_otro_metodo() {
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.EFECTIVO,
                DeliveryType.RETIRO, null, null, null, null,
                List.of(item("asado", new BigDecimal("18500"), BigDecimal.ONE)), Instant.now());

        assertThatThrownBy(order::confirmMercadoPagoPayment).isInstanceOf(IllegalStateException.class);
    }
}
