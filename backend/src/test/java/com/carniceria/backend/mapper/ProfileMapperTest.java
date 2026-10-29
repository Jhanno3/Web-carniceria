package com.carniceria.backend.mapper;

import com.carniceria.backend.dto.request.UpdateProfileRequestDto;
import com.carniceria.backend.entity.ProfileEntity;
import com.carniceria.backend.dto.response.ProfileResponseDto;
import com.carniceria.backend.model.Profile;
import com.carniceria.backend.model.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileMapperTest {

    private final ProfileMapper mapper = new ProfileMapper();

    @Test
    void entity_a_model_copia_todos_los_campos() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        ProfileEntity entity = new ProfileEntity();
        entity.setId(id);
        entity.setRole("admin");
        entity.setFirstName("Ana");
        entity.setLastName("Gomez");
        entity.setPhone("+5491112345678");
        entity.setAddressText("Calle Falsa 123");
        entity.setAddressLat(-34.6);
        entity.setAddressLng(-58.4);
        entity.setCreatedAt(now);

        Profile model = mapper.toModel(entity);

        assertThat(model.id()).isEqualTo(id);
        assertThat(model.role()).isEqualTo(Role.ADMIN);
        assertThat(model.firstName()).isEqualTo("Ana");
        assertThat(model.addressLat()).isEqualTo(-34.6);
        assertThat(model.createdAt()).isEqualTo(now);
    }

    @Test
    void model_a_entity_copia_todos_los_campos() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Profile model = new Profile(id, Role.CLIENT, "Juan", "Perez", "+5491112345678",
                "Otra Calle 456", -34.5, -58.3, now);

        ProfileEntity entity = mapper.toEntity(model);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getRole()).isEqualTo("client");
        assertThat(entity.getFirstName()).isEqualTo("Juan");
        assertThat(entity.getAddressLng()).isEqualTo(-58.3);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void model_a_response_dto_nunca_expone_mas_de_lo_necesario() {
        Profile model = new Profile(UUID.randomUUID(), Role.CLIENT, "Juan", "Perez", "+5491112345678",
                null, null, null, Instant.now());

        ProfileResponseDto dto = mapper.toResponseDto(model);

        assertThat(dto.firstName()).isEqualTo("Juan");
        assertThat(dto.role()).isEqualTo("client");
    }

    @Test
    void aplicar_update_solo_cambia_los_datos_de_contacto() {
        UUID id = UUID.randomUUID();
        Profile existente = new Profile(id, Role.ADMIN, "Viejo", "Nombre", null, null, null, null, Instant.now());
        UpdateProfileRequestDto dto = new UpdateProfileRequestDto("Nuevo", "Apellido", "+5491112345678",
                "Dirección nueva", -34.1, -58.1);

        Profile actualizado = mapper.applyUpdate(existente, dto);

        assertThat(actualizado.id()).isEqualTo(id);
        assertThat(actualizado.role()).isEqualTo(Role.ADMIN);
        assertThat(actualizado.firstName()).isEqualTo("Nuevo");
        assertThat(actualizado.addressText()).isEqualTo("Dirección nueva");
    }
}
