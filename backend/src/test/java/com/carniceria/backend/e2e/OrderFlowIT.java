package com.carniceria.backend.e2e;

import com.carniceria.backend.TestcontainersConfiguration;
import com.carniceria.backend.entity.ProductEntity;
import com.carniceria.backend.entity.ProfileEntity;
import com.carniceria.backend.repository.ProductRepository;
import com.carniceria.backend.repository.ProfileRepository;
import com.carniceria.backend.security.TestJwtConfiguration;
import com.carniceria.backend.security.TestJwtSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo de pedidos: HTTP -> controller -> service -> repository -> Postgres real.
 * Mismo patrón que AuthProfileFlowIT (feature 001): ddl-auto=create-drop porque este Postgres
 * de Testcontainers no tiene el esquema de Supabase, JWT firmados localmente en vez del JWKS real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestJwtConfiguration.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class OrderFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void pedido_en_efectivo_camino_feliz() throws Exception {
        UUID clientId = seedClient();
        seedProduct("asado", "Asado", "kg", new BigDecimal("18500"));
        String token = TestJwtSupport.signedJwtFor(clientId);

        String body = """
                {"paymentMethod":"efectivo","deliveryType":"retiro","items":[{"productId":"asado","quantity":2}]}
                """;

        mockMvc.perform(post("/api/orders").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("confirmado"))
                .andExpect(jsonPath("$.paymentStatus").value("pendiente"))
                .andExpect(jsonPath("$.totalEstimated").value(37000));
    }

    @Test
    void pedido_por_transferencia_se_marca_y_el_admin_lo_confirma() throws Exception {
        UUID clientId = seedClient();
        UUID adminId = seedAdmin();
        seedProduct("vacio", "Vacío", "kg", new BigDecimal("20000"));
        String clientToken = TestJwtSupport.signedJwtFor(clientId);
        String adminToken = TestJwtSupport.signedJwtFor(adminId);

        String body = """
                {"paymentMethod":"transferencia","deliveryType":"retiro","items":[{"productId":"vacio","quantity":1}]}
                """;

        MvcResult createResult = mockMvc.perform(post("/api/orders").header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pendiente_pago"))
                .andReturn();
        UUID orderId = orderIdFrom(createResult);

        mockMvc.perform(put("/api/orders/" + orderId + "/mark-transfer-sent")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pago_en_revision"));

        mockMvc.perform(put("/api/admin/orders/" + orderId + "/confirm-transfer")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.status").value("confirmado"))
                .andExpect(jsonPath("$.order.paymentStatus").value("completado"));
    }

    @Test
    void un_cliente_no_puede_ver_ni_marcar_el_pedido_de_otro() throws Exception {
        UUID clienteA = seedClient();
        UUID clienteB = seedClient();
        seedProduct("asado", "Asado", "kg", new BigDecimal("18500"));
        String tokenA = TestJwtSupport.signedJwtFor(clienteA);
        String tokenB = TestJwtSupport.signedJwtFor(clienteB);

        String body = """
                {"paymentMethod":"efectivo","deliveryType":"retiro","items":[{"productId":"asado","quantity":1}]}
                """;
        MvcResult createResult = mockMvc.perform(post("/api/orders").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        UUID orderId = orderIdFrom(createResult);

        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/orders/" + orderId + "/mark-transfer-sent").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    void un_cliente_no_puede_listar_los_pedidos_de_admin() throws Exception {
        UUID clientId = seedClient();
        String token = TestJwtSupport.signedJwtFor(clientId);

        mockMvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private UUID orderIdFrom(MvcResult result) throws Exception {
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return UUID.fromString(id);
    }

    private UUID seedClient() {
        return seedProfile("client", "Juan", "Perez", "+5491112345678");
    }

    private UUID seedAdmin() {
        return seedProfile("admin", "Admin", "Uno", "+5491112345678");
    }

    private UUID seedProfile(String role, String firstName, String lastName, String phone) {
        UUID id = UUID.randomUUID();
        ProfileEntity profile = new ProfileEntity();
        profile.setId(id);
        profile.setRole(role);
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setPhone(phone);
        profile.setCreatedAt(Instant.now());
        profileRepository.save(profile);
        return id;
    }

    private void seedProduct(String id, String name, String unit, BigDecimal price) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setCategory("carne");
        product.setName(name);
        product.setUnit(unit);
        product.setPricePerUnit(price);
        product.setActive(true);
        product.setCreatedAt(Instant.now());
        productRepository.save(product);
    }
}
