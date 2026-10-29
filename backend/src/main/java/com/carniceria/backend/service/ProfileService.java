package com.carniceria.backend.service;

import com.carniceria.backend.dto.request.UpdateProfileRequestDto;
import com.carniceria.backend.entity.ProfileEntity;
import com.carniceria.backend.exception.ForbiddenException;
import com.carniceria.backend.exception.ProfileNotFoundException;
import com.carniceria.backend.mapper.ProfileMapper;
import com.carniceria.backend.model.Profile;
import com.carniceria.backend.model.Role;
import com.carniceria.backend.repository.ProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProfileService {

    private final ProfileRepository repository;
    private final ProfileMapper mapper;

    public ProfileService(ProfileRepository repository, ProfileMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Profile getMyProfile(UUID userId) {
        return findOrThrow(userId);
    }

    public Profile updateMyProfile(UUID userId, UpdateProfileRequestDto dto) {
        Profile existing = findOrThrow(userId);
        Profile updated = mapper.applyUpdate(existing, dto);
        repository.save(mapper.toEntity(updated));
        return updated;
    }

    /** Solo el propio usuario o un admin pueden ver un perfil ajeno. */
    public Profile getProfileById(UUID requesterId, Role requesterRole, UUID targetId) {
        if (requesterRole != Role.ADMIN && !requesterId.equals(targetId)) {
            throw new ForbiddenException("No podés ver el perfil de otro usuario.");
        }
        return findOrThrow(targetId);
    }

    public List<Profile> listAllProfiles(Role requesterRole) {
        if (requesterRole != Role.ADMIN) {
            throw new ForbiddenException("Solo el admin puede listar todos los perfiles.");
        }
        return repository.findAll().stream().map(mapper::toModel).toList();
    }

    private Profile findOrThrow(UUID id) {
        ProfileEntity entity = repository.findById(id).orElseThrow(() -> new ProfileNotFoundException(id));
        return mapper.toModel(entity);
    }
}
