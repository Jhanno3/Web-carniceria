package com.carniceria.backend.mapper;

import com.carniceria.backend.dto.response.OrderResponseDto;
import com.carniceria.backend.entity.OrderEntity;
import com.carniceria.backend.entity.OrderItemEntity;
import com.carniceria.backend.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    private Order sampleOrder() {
        OrderItem item = OrderItem.of("asado", "Asado", Unit.KG, new BigDecimal("2"), new BigDecimal("18500"), "banderita");
        return Order.create(UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.EFECTIVO, DeliveryType.RETIRO,
                null, null, null, "sin notas", List.of(item), Instant.now());
    }

    @Test
    void model_a_entity_y_de_vuelta_conserva_los_items() {
        Order order = sampleOrder();

        OrderEntity entity = mapper.toEntity(order);
        assertThat(entity.getItems()).hasSize(1);
        assertThat(entity.getItems().get(0).getOrder()).isSameAs(entity);

        Order reconstruido = mapper.toModel(entity);
        assertThat(reconstruido.items()).hasSize(1);
        assertThat(reconstruido.items().get(0).productId()).isEqualTo("asado");
        assertThat(reconstruido.totalEstimated()).isEqualByComparingTo("37000");
        assertThat(reconstruido.status()).isEqualTo(OrderStatus.CONFIRMADO);
    }

    @Test
    void entity_a_model_parsea_los_enums_correctamente() {
        OrderEntity entity = new OrderEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(UUID.randomUUID());
        entity.setStatus("pago_en_revision");
        entity.setPaymentMethod("transferencia");
        entity.setPaymentStatus("pendiente");
        entity.setDeliveryType("envio");
        entity.setTotalEstimated(new BigDecimal("1000"));
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        OrderItemEntity itemEntity = new OrderItemEntity();
        itemEntity.setId(UUID.randomUUID());
        itemEntity.setProductId("vacio");
        itemEntity.setProductNameSnapshot("Vacío");
        itemEntity.setUnit("kg");
        itemEntity.setQuantity(new BigDecimal("1"));
        itemEntity.setPricePerUnitSnapshot(new BigDecimal("1000"));
        entity.addItem(itemEntity);

        Order model = mapper.toModel(entity);

        assertThat(model.status()).isEqualTo(OrderStatus.PAGO_EN_REVISION);
        assertThat(model.paymentMethod()).isEqualTo(PaymentMethod.TRANSFERENCIA);
        assertThat(model.deliveryType()).isEqualTo(DeliveryType.ENVIO);
    }

    @Test
    void model_a_response_dto_incluye_los_items() {
        OrderResponseDto dto = mapper.toResponseDto(sampleOrder());

        assertThat(dto.status()).isEqualTo("confirmado");
        assertThat(dto.items()).hasSize(1);
        assertThat(dto.items().get(0).itemNotes()).isEqualTo("banderita");
        assertThat(dto.totalEstimated()).isEqualByComparingTo("37000");
    }
}
