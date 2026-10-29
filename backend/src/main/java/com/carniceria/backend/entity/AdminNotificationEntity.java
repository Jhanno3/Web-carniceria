package com.carniceria.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_notifications")
@Getter
@Setter
@NoArgsConstructor
public class AdminNotificationEntity {

    @Id
    private UUID id;

    private UUID orderId;
    private String type;
    private boolean read;
    private Instant createdAt;
}
