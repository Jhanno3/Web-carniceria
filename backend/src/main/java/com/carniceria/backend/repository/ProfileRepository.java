package com.carniceria.backend.repository;

import com.carniceria.backend.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProfileRepository extends JpaRepository<ProfileEntity, UUID> {
}
