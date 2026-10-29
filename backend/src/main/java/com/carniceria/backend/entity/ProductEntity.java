package com.carniceria.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class ProductEntity {

    @Id
    private String id;

    private String category;
    private String name;
    private String detail;
    private String unit;
    private BigDecimal pricePerUnit;
    private String tag;
    private boolean active;
    private Instant createdAt;
}
