package com.carniceria.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

    @Id
    private UUID id;

    private UUID userId;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private String deliveryType;
    private String deliveryAddressText;
    private Double deliveryAddressLat;
    private Double deliveryAddressLng;
    private BigDecimal totalEstimated;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItemEntity> items = new ArrayList<>();

    public void addItem(OrderItemEntity item) {
        item.setOrder(this);
        items.add(item);
    }
}
