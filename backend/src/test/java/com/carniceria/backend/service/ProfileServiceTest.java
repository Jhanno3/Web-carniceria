package com.carniceria.backend.service;

import com.carniceria.backend.dto.request.UpdateProfileRequestDto;
import com.carniceria.backend.entity.ProfileEntity;
import com.carniceria.backend.exception.ForbiddenException;
import com.carniceria.backend.exception.ProfileNotFoundException;
import com.carniceria.backend.mapper.ProfileMapper;
import com.carniceria.backend.model.Profile;
import com.carniceria.backend.model.Role;
import com.carniceria.backend.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository repository;

    private final ProfileMapper mapper = new ProfileMapper();

    private ProfileService service;

    private ProfileEntity entityFor(UUID id, Role role) {
        ProfileEntity e = new ProfileEntity();
        e.setId(id);
        e.setRole(role.name().toLowerCase());
        e.setCreatedAt(Instant.now());
        return e;
    }

    @Test
    void getMyProfile_camino_feliz() {
        service = new ProfileService(repository, mapper);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(entityFor(id, Role.CLIENT)));

        Profile profile = service.getMyProfile(id);

        assertThat(profile.id()).isEqualTo(id);
    }

    @Test
    void getMyProfile_lanza_notfound_si_no_existe() {
        service = new ProfileService(repository, mapper);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyProfile(id)).isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void updateMyProfile_ignora_cualquier_intento_de_cambiar_el_role() {
        service = new ProfileService(repository, mapper);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(entityFor(id, Role.ADMIN)));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequestDto dto = new UpdateProfileRequestDto("Nuevo", "Nombre", "+5491112345678", null, null, null);
        Profile actualizado = service.updateMyProfile(id, dto);

        assertThat(actualizado.role()).isEqualTo(Role.ADMIN);
        verify(repository).save(any());
    }

    @Test
    void getProfileById_rechaza_a_un_client_pidiendo_el_perfil_de_otro_client() {
        service = new ProfileService(repository, mapper);
        UUID yo = UUID.randomUUID();
        UUID otro = UUID.randomUUID();

        assertThatThrownBy(() -> service.getProfileById(yo, Role.CLIENT, otro))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getProfileById_permite_a_un_client_ver_su_propio_perfil() {
        service = new ProfileService(repository, mapper);
        UUID yo = UUID.randomUUID();
        when(repository.findById(yo)).thenReturn(Optional.of(entityFor(yo, Role.CLIENT)));

        Profile profile = service.getProfileById(yo, Role.CLIENT, yo);

        assertThat(profile.id()).isEqualTo(yo);
    }

    @Test
    void getProfileById_permite_a_un_admin_ver_cualquier_perfil() {
        service = new ProfileService(repository, mapper);
        UUID admin = UUID.randomUUID();
        UUID otro = UUID.randomUUID();
        when(repository.findById(otro)).thenReturn(Optional.of(entityFor(otro, Role.CLIENT)));

        Profile profile = service.getProfileById(admin, Role.ADMIN, otro);

        assertThat(profile.id()).isEqualTo(otro);
    }

    @Test
    void listAllProfiles_rechaza_a_quien_no_es_admin() {
        service = new ProfileService(repository, mapper);

        assertThatThrownBy(() -> service.listAllProfiles(Role.CLIENT)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listAllProfiles_devuelve_todos_para_un_admin() {
        service = new ProfileService(repository, mapper);
        when(repository.findAll()).thenReturn(List.of(entityFor(UUID.randomUUID(), Role.CLIENT), entityFor(UUID.randomUUID(), Role.CLIENT)));

        List<Profile> perfiles = service.listAllProfiles(Role.ADMIN);

        assertThat(perfiles).hasSize(2);
    }
}
