package com.carniceria.backend.mapper;

import com.carniceria.backend.dto.request.UpdateProfileRequestDto;
import com.carniceria.backend.dto.response.ProfileResponseDto;
import com.carniceria.backend.entity.ProfileEntity;
import com.carniceria.backend.model.Profile;
import com.carniceria.backend.model.Role;
import org.springframework.stereotype.Component;

/** Traducción explícita entre dto, model y entity. Ninguna otra capa convierte entre estas tres formas. */
@Component
public class ProfileMapper {

    public Profile toModel(ProfileEntity entity) {
        return new Profile(
                entity.getId(),
                Role.valueOf(entity.getRole().toUpperCase()),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getPhone(),
                entity.getAddressText(),
                entity.getAddressLat(),
                entity.getAddressLng(),
                entity.getCreatedAt()
        );
    }

    public ProfileEntity toEntity(Profile model) {
        ProfileEntity entity = new ProfileEntity();
        entity.setId(model.id());
        entity.setRole(model.role().name().toLowerCase());
        entity.setFirstName(model.firstName());
        entity.setLastName(model.lastName());
        entity.setPhone(model.phone());
        entity.setAddressText(model.addressText());
        entity.setAddressLat(model.addressLat());
        entity.setAddressLng(model.addressLng());
        entity.setCreatedAt(model.createdAt());
        return entity;
    }

    public ProfileResponseDto toResponseDto(Profile model) {
        return new ProfileResponseDto(
                model.id(),
                model.role().name().toLowerCase(),
                model.firstName(),
                model.lastName(),
                model.phone(),
                model.addressText(),
                model.addressLat(),
                model.addressLng(),
                model.createdAt()
        );
    }

    public Profile applyUpdate(Profile existing, UpdateProfileRequestDto dto) {
        return existing.withContactInfo(
                dto.firstName(),
                dto.lastName(),
                dto.phone(),
                dto.addressText(),
                dto.addressLat(),
                dto.addressLng()
        );
    }
}
