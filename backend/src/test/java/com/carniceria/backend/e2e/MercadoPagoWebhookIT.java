package com.carniceria.backend.e2e;

import com.carniceria.backend.TestcontainersConfiguration;
import com.carniceria.backend.entity.ProductEntity;
import com.carniceria.backend.entity.ProfileEntity;
import com.carniceria.backend.repository.ProductRepository;
import com.carniceria.backend.repository.ProfileRepository;
import com.carniceria.backend.security.TestJwtConfiguration;
import com.carniceria.backend.security.TestJwtSupport;
import com.carniceria.backend.service.MercadoPagoService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Único IT que mockea algo externo: MercadoPagoService, porque el límite real del sistema acá
 * es la API de Mercado Pago, no nuestra base (ver plan.md de specs/003-mercadopago). Todo lo
 * demás (Postgres, HTTP, JWT) sigue siendo real, mismo patrón que AuthProfileFlowIT/OrderFlowIT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestJwtConfiguration.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class MercadoPagoWebhookIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private MercadoPagoService mercadoPagoService;

    @Test
    void crear_pedido_con_mercadopago_devuelve_el_init_point_de_la_preferencia() throws Exception {
        UUID clientId = seedClient();
        seedProduct("asado", "Asado", new BigDecimal("18500"));
        when(mercadoPagoService.createPreference(any()))
                .thenReturn(new MercadoPagoService.PreferenceResult("pref-1", "https://mercadopago.test/pay/pref-1"));
        String token = TestJwtSupport.signedJwtFor(clientId);

        String body = """
                {"paymentMethod":"mercadopago","deliveryType":"retiro","items":[{"productId":"asado","quantity":1}]}
                """;

        mockMvc.perform(post("/api/orders").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pendiente_pago"))
                .andExpect(jsonPath("$.mercadopagoInitPoint").value("https://mercadopago.test/pay/pref-1"));
    }

    @Test
    void webhook_de_pago_aprobado_confirma_el_pedido_correcto() throws Exception {
        UUID clientId = seedClient();
        seedProduct("vacio", "Vacío", new BigDecimal("20000"));
        when(mercadoPagoService.createPreference(any()))
                .thenReturn(new MercadoPagoService.PreferenceResult("pref-2", "https://mercadopago.test/pay/pref-2"));
        String token = TestJwtSupport.signedJwtFor(clientId);

        String body = """
                {"paymentMethod":"mercadopago","deliveryType":"retiro","items":[{"productId":"vacio","quantity":1}]}
                """;
        MvcResult createResult = mockMvc.perform(post("/api/orders").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        String orderId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        when(mercadoPagoService.getPayment("mp-payment-1"))
                .thenReturn(new MercadoPagoService.MercadoPagoPaymentInfo("mp-payment-1", "approved", orderId));

        String webhookBody = """
                {"type":"payment","data":{"id":"mp-payment-1"}}
                """;
        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON).content(webhookBody))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("confirmado"))
                .andExpect(jsonPath("$.paymentStatus").value("completado"));
    }

    @Test
    void webhook_con_referencia_desconocida_no_rompe_nada() throws Exception {
        when(mercadoPagoService.getPayment("mp-payment-999"))
                .thenReturn(new MercadoPagoService.MercadoPagoPaymentInfo("mp-payment-999", "approved", UUID.randomUUID().toString()));

        String webhookBody = """
                {"type":"payment","data":{"id":"mp-payment-999"}}
                """;
        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON).content(webhookBody))
                .andExpect(status().isOk());
    }

    @Test
    void webhook_sin_autenticacion_igual_responde_porque_es_publico() throws Exception {
        String webhookBody = """
                {"type":"merchant_order","data":{"id":"123"}}
                """;
        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON).content(webhookBody))
                .andExpect(status().isOk());
    }

    private UUID seedClient() {
        UUID id = UUID.randomUUID();
        ProfileEntity profile = new ProfileEntity();
        profile.setId(id);
        profile.setRole("client");
        profile.setFirstName("Juan");
        profile.setLastName("Perez");
        profile.setPhone("+5491112345678");
        profile.setCreatedAt(Instant.now());
        profileRepository.save(profile);
        return id;
    }

    private void seedProduct(String id, String name, BigDecimal price) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setCategory("carne");
        product.setName(name);
        product.setUnit("kg");
        product.setPricePerUnit(price);
        product.setActive(true);
        product.setCreatedAt(Instant.now());
        productRepository.save(product);
    }
}
