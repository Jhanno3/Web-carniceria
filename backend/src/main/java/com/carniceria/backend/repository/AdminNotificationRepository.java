package com.carniceria.backend.repository;

import com.carniceria.backend.entity.AdminNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminNotificationRepository extends JpaRepository<AdminNotificationEntity, UUID> {
}
