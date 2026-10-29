package com.carniceria.backend.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileTest {

    private Profile blankProfile() {
        return new Profile(UUID.randomUUID(), Role.CLIENT, null, null, null, null, null, null, Instant.now());
    }

    @Test
    void rechaza_telefono_con_formato_invalido() {
        assertThatThrownBy(() -> blankProfile().withContactInfo("Juan", "Perez", "no-es-un-telefono", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acepta_telefono_vacio_porque_todavia_no_es_obligatorio() {
        Profile p = blankProfile().withContactInfo(null, null, null, null, null, null);
        assertThat(p.phone()).isNull();
    }

    @Test
    void perfil_incompleto_si_falta_nombre_apellido_o_telefono() {
        Profile sinNada = blankProfile();
        assertThat(sinNada.isComplete()).isFalse();

        Profile soloNombre = sinNada.withContactInfo("Juan", null, null, null, null, null);
        assertThat(soloNombre.isComplete()).isFalse();
    }

    @Test
    void perfil_completo_con_nombre_apellido_y_telefono() {
        Profile completo = blankProfile().withContactInfo("Juan", "Perez", "+54 11 1234-5678", null, null, null);
        assertThat(completo.isComplete()).isTrue();
    }

    @Test
    void actualizar_datos_de_contacto_nunca_cambia_el_id_ni_el_role() {
        UUID id = UUID.randomUUID();
        Profile admin = new Profile(id, Role.ADMIN, null, null, null, null, null, null, Instant.now());

        Profile actualizado = admin.withContactInfo("Ana", "Gomez", "+5491112345678", "Calle Falsa 123", -34.6, -58.4);

        assertThat(actualizado.id()).isEqualTo(id);
        assertThat(actualizado.role()).isEqualTo(Role.ADMIN);
        assertThat(actualizado.firstName()).isEqualTo("Ana");
        assertThat(actualizado.addressText()).isEqualTo("Calle Falsa 123");
    }
}
