package com.carniceria.backend.e2e;

import com.carniceria.backend.TestcontainersConfiguration;
import com.carniceria.backend.entity.ProfileEntity;
import com.carniceria.backend.repository.ProfileRepository;
import com.carniceria.backend.security.TestJwtConfiguration;
import com.carniceria.backend.security.TestJwtSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo HTTP -> controller -> service -> repository -> Postgres real (Testcontainers),
 * con JWT firmados localmente (ver TestJwtSupport) en vez del JWKS real de Supabase.
 * ddl-auto=create-drop solo en este test: Hibernate crea el esquema a partir de ProfileEntity,
 * ya que este Postgres de test no tiene el esquema de Supabase (ni su RLS, que se valida aparte,
 * a mano, en el SQL de supabase/migrations). Acá se prueba la autorización de la capa service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestJwtConfiguration.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class AuthProfileFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileRepository profileRepository;

    @Test
    void camino_feliz_get_y_put_profile_me() throws Exception {
        UUID userId = UUID.randomUUID();
        seedProfile(userId, "client");
        String token = TestJwtSupport.signedJwtFor(userId);

        mockMvc.perform(get("/api/profile/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));

        String body = "{\"firstName\":\"Juan\",\"lastName\":\"Perez\",\"phone\":\"+5491112345678\"}";

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Juan"));
    }

    @Test
    void client_no_puede_leer_el_perfil_de_otro_client() throws Exception {
        UUID clienteA = UUID.randomUUID();
        UUID clienteB = UUID.randomUUID();
        seedProfile(clienteA, "client");
        seedProfile(clienteB, "client");
        String tokenDeA = TestJwtSupport.signedJwtFor(clienteA);

        mockMvc.perform(get("/api/profiles/" + clienteB).header("Authorization", "Bearer " + tokenDeA))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_lista_todos_los_perfiles() throws Exception {
        UUID admin = UUID.randomUUID();
        seedProfile(admin, "admin");
        seedProfile(UUID.randomUUID(), "client");
        seedProfile(UUID.randomUUID(), "client");
        String token = TestJwtSupport.signedJwtFor(admin);

        mockMvc.perform(get("/api/profiles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void mandar_role_en_el_body_de_put_no_cambia_el_rol() throws Exception {
        UUID admin = UUID.randomUUID();
        seedProfile(admin, "admin");
        String token = TestJwtSupport.signedJwtFor(admin);

        String body = "{\"firstName\":\"Admin\",\"lastName\":\"Uno\",\"phone\":\"+5491112345678\",\"role\":\"client\"}";

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        assertThat(profileRepository.findById(admin).orElseThrow().getRole()).isEqualTo("admin");
    }

    private void seedProfile(UUID id, String role) {
        ProfileEntity entity = new ProfileEntity();
        entity.setId(id);
        entity.setRole(role);
        entity.setCreatedAt(Instant.now());
        profileRepository.save(entity);
    }
}
