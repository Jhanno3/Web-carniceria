package com.carniceria.backend.mapper;

import com.carniceria.backend.dto.response.AdminOrderResponseDto;
import com.carniceria.backend.dto.response.OrderItemResponseDto;
import com.carniceria.backend.dto.response.OrderResponseDto;
import com.carniceria.backend.entity.OrderEntity;
import com.carniceria.backend.entity.OrderItemEntity;
import com.carniceria.backend.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class OrderMapper {

    public Order toModel(OrderEntity entity) {
        List<OrderItem> items = entity.getItems().stream().map(this::itemToModel).toList();
        return new Order(
                entity.getId(),
                entity.getUserId(),
                OrderStatus.valueOf(entity.getStatus().toUpperCase()),
                PaymentMethod.valueOf(entity.getPaymentMethod().toUpperCase()),
                PaymentStatus.valueOf(entity.getPaymentStatus().toUpperCase()),
                DeliveryType.valueOf(entity.getDeliveryType().toUpperCase()),
                entity.getDeliveryAddressText(),
                entity.getDeliveryAddressLat(),
                entity.getDeliveryAddressLng(),
                entity.getTotalEstimated(),
                entity.getNotes(),
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private OrderItem itemToModel(OrderItemEntity e) {
        return OrderItem.of(e.getProductId(), e.getProductNameSnapshot(), Unit.valueOf(e.getUnit().toUpperCase()),
                e.getQuantity(), e.getPricePerUnitSnapshot(), e.getItemNotes());
    }

    public OrderEntity toEntity(Order model) {
        OrderEntity entity = new OrderEntity();
        entity.setId(model.id());
        entity.setUserId(model.userId());
        entity.setStatus(model.status().name().toLowerCase());
        entity.setPaymentMethod(model.paymentMethod().name().toLowerCase());
        entity.setPaymentStatus(model.paymentStatus().name().toLowerCase());
        entity.setDeliveryType(model.deliveryType().name().toLowerCase());
        entity.setDeliveryAddressText(model.deliveryAddressText());
        entity.setDeliveryAddressLat(model.deliveryAddressLat());
        entity.setDeliveryAddressLng(model.deliveryAddressLng());
        entity.setTotalEstimated(model.totalEstimated());
        entity.setNotes(model.notes());
        entity.setCreatedAt(model.createdAt());
        entity.setUpdatedAt(model.updatedAt());
        for (OrderItem item : model.items()) {
            entity.addItem(itemToEntity(item));
        }
        return entity;
    }

    private OrderItemEntity itemToEntity(OrderItem item) {
        OrderItemEntity entity = new OrderItemEntity();
        entity.setId(UUID.randomUUID());
        entity.setProductId(item.productId());
        entity.setProductNameSnapshot(item.productNameSnapshot());
        entity.setUnit(item.unit().name().toLowerCase());
        entity.setQuantity(item.quantity());
        entity.setPricePerUnitSnapshot(item.pricePerUnitSnapshot());
        entity.setSubtotalEstimated(item.subtotalEstimated());
        entity.setItemNotes(item.itemNotes());
        return entity;
    }

    public OrderResponseDto toResponseDto(Order model) {
        return toResponseDto(model, null);
    }

    public OrderResponseDto toResponseDto(Order model, String mercadopagoInitPoint) {
        List<OrderItemResponseDto> items = model.items().stream()
                .map(i -> new OrderItemResponseDto(i.productId(), i.productNameSnapshot(), i.unit().name().toLowerCase(),
                        i.quantity(), i.pricePerUnitSnapshot(), i.subtotalEstimated(), i.itemNotes()))
                .toList();
        return new OrderResponseDto(model.id(), model.status().name().toLowerCase(),
                model.paymentMethod().name().toLowerCase(), model.paymentStatus().name().toLowerCase(),
                model.deliveryType().name().toLowerCase(), model.deliveryAddressText(), model.deliveryAddressLat(),
                model.deliveryAddressLng(), model.totalEstimated(), model.shippingCost(), model.notes(), items,
                model.createdAt(), mercadopagoInitPoint);
    }

    public AdminOrderResponseDto toAdminResponseDto(Order model, String clientFirstName, String clientLastName, String clientPhone) {
        return new AdminOrderResponseDto(toResponseDto(model), clientFirstName, clientLastName, clientPhone);
    }
}
