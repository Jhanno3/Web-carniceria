package com.carniceria.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class PaymentEntity {

    @Id
    private UUID id;

    private UUID orderId;
    private String method;
    private String status;
    private String mercadopagoPaymentId;
    private String mercadopagoPreferenceId;
    private Instant transferMarkedByClientAt;
    private BigDecimal amount;
    private Instant createdAt;
    private Instant confirmedAt;
}
