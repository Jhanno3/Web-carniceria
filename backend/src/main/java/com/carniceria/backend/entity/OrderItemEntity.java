package com.carniceria.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItemEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    private String productId;
    private String productNameSnapshot;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal pricePerUnitSnapshot;
    private BigDecimal subtotalEstimated;
    private String itemNotes;
}
