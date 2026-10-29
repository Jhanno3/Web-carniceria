package com.carniceria.backend.service;

import com.carniceria.backend.dto.request.CreateOrderRequestDto;
import com.carniceria.backend.dto.request.OrderItemRequestDto;
import com.carniceria.backend.entity.OrderEntity;
import com.carniceria.backend.entity.OrderItemEntity;
import com.carniceria.backend.entity.PaymentEntity;
import com.carniceria.backend.entity.ProductEntity;
import com.carniceria.backend.entity.ProfileEntity;
import com.carniceria.backend.exception.ForbiddenException;
import com.carniceria.backend.mapper.OrderMapper;
import com.carniceria.backend.model.Order;
import com.carniceria.backend.model.Role;
import com.carniceria.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private AdminNotificationRepository notificationRepository;
    @Mock private MercadoPagoService mercadoPagoService;

    private final OrderMapper mapper = new OrderMapper();

    private OrderService service() {
        return new OrderService(orderRepository, productRepository, profileRepository, paymentRepository,
                notificationRepository, mercadoPagoService, mapper);
    }

    private ProfileEntity completeProfile(UUID id) {
        ProfileEntity p = new ProfileEntity();
        p.setId(id);
        p.setRole("client");
        p.setFirstName("Juan");
        p.setLastName("Perez");
        p.setPhone("+5491112345678");
        p.setCreatedAt(Instant.now());
        return p;
    }

    private ProductEntity product(String id, String name, String unit, BigDecimal price) {
        ProductEntity p = new ProductEntity();
        p.setId(id);
        p.setCategory("carne");
        p.setName(name);
        p.setUnit(unit);
        p.setPricePerUnit(price);
        p.setActive(true);
        return p;
    }

    private OrderEntity orderEntity(UUID id, UUID userId, String status, String paymentMethod) {
        OrderEntity entity = new OrderEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setStatus(status);
        entity.setPaymentMethod(paymentMethod);
        entity.setPaymentStatus("pendiente");
        entity.setDeliveryType("retiro");
        entity.setTotalEstimated(BigDecimal.TEN);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        OrderItemEntity item = new OrderItemEntity();
        item.setId(UUID.randomUUID());
        item.setProductId("asado");
        item.setProductNameSnapshot("Asado");
        item.setUnit("kg");
        item.setQuantity(BigDecimal.ONE);
        entity.addItem(item);
        return entity;
    }

    @Test
    void createOrder_usa_el_precio_del_producto_en_la_base_no_hay_forma_de_mandar_otro() {
        // El DTO ni siquiera tiene un campo de precio: estructuralmente no se puede mandar uno
        // manipulado. Este test prueba que el precio sale de ProductRepository.
        UUID userId = UUID.randomUUID();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(completeProfile(userId)));
        when(productRepository.findById("asado")).thenReturn(Optional.of(product("asado", "Asado", "kg", new BigDecimal("18500"))));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequestDto dto = new CreateOrderRequestDto("efectivo", "retiro", null, null, null, null,
                List.of(new OrderItemRequestDto("asado", new BigDecimal("2"), null)));

        Order order = service().createOrder(userId, dto).order();

        assertThat(order.totalEstimated()).isEqualByComparingTo("37000");
        assertThat(order.items().get(0).pricePerUnitSnapshot()).isEqualByComparingTo("18500");
    }

    @Test
    void createOrder_rechaza_un_producto_que_no_existe() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(completeProfile(userId)));
        when(productRepository.findById("no-existe")).thenReturn(Optional.empty());

        CreateOrderRequestDto dto = new CreateOrderRequestDto("efectivo", "retiro", null, null, null, null,
                List.of(new OrderItemRequestDto("no-existe", BigDecimal.ONE, null)));

        assertThatThrownBy(() -> service().createOrder(userId, dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createOrder_rechaza_si_el_perfil_esta_incompleto() {
        UUID userId = UUID.randomUUID();
        ProfileEntity incompleto = new ProfileEntity();
        incompleto.setId(userId);
        incompleto.setRole("client");
        incompleto.setCreatedAt(Instant.now());
        when(profileRepository.findById(userId)).thenReturn(Optional.of(incompleto));

        CreateOrderRequestDto dto = new CreateOrderRequestDto("efectivo", "retiro", null, null, null, null,
                List.of(new OrderItemRequestDto("asado", BigDecimal.ONE, null)));

        assertThatThrownBy(() -> service().createOrder(userId, dto)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createOrder_rechaza_envio_sin_direccion() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(completeProfile(userId)));

        CreateOrderRequestDto dto = new CreateOrderRequestDto("efectivo", "envio", null, null, null, null,
                List.of(new OrderItemRequestDto("asado", BigDecimal.ONE, null)));

        assertThatThrownBy(() -> service().createOrder(userId, dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createOrder_con_mercadopago_pide_la_preferencia_y_devuelve_el_init_point() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findById(userId)).thenReturn(Optional.of(completeProfile(userId)));
        when(productRepository.findById("asado")).thenReturn(Optional.of(product("asado", "Asado", "kg", new BigDecimal("18500"))));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByOrderId(any())).thenReturn(Optional.of(new PaymentEntity()));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mercadoPagoService.createPreference(any()))
                .thenReturn(new MercadoPagoService.PreferenceResult("pref-123", "https://mercadopago.test/pay/pref-123"));

        CreateOrderRequestDto dto = new CreateOrderRequestDto("mercadopago", "retiro", null, null, null, null,
                List.of(new OrderItemRequestDto("asado", BigDecimal.ONE, null)));

        OrderService.OrderCreationResult result = service().createOrder(userId, dto);

        assertThat(result.mercadopagoInitPoint()).isEqualTo("https://mercadopago.test/pay/pref-123");
        verify(mercadoPagoService).createPreference(any());
    }

    @Test
    void markTransferSent_rechaza_a_quien_no_es_el_dueno_del_pedido() {
        UUID dueño = UUID.randomUUID();
        UUID otro = UUID.randomUUID();
        OrderEntity entity = orderEntity(UUID.randomUUID(), dueño, "pendiente_pago", "transferencia");
        when(orderRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service().markTransferSent(otro, entity.getId())).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listAllOrders_rechaza_a_quien_no_es_admin() {
        assertThatThrownBy(() -> service().listAllOrders(Role.CLIENT)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void confirmTransferPayment_rechaza_a_quien_no_es_admin() {
        assertThatThrownBy(() -> service().confirmTransferPayment(Role.CLIENT, UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void webhook_ignora_un_pedido_que_no_existe() {
        when(mercadoPagoService.getPayment("999"))
                .thenReturn(new MercadoPagoService.MercadoPagoPaymentInfo("999", "approved", UUID.randomUUID().toString()));

        service().handleMercadoPagoWebhook("999");

        verify(orderRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void webhook_confirma_el_pedido_correcto_cuando_el_pago_esta_aprobado() {
        UUID userId = UUID.randomUUID();
        OrderEntity entity = orderEntity(UUID.randomUUID(), userId, "pendiente_pago", "mercadopago");
        when(mercadoPagoService.getPayment("555"))
                .thenReturn(new MercadoPagoService.MercadoPagoPaymentInfo("555", "approved", entity.getId().toString()));
        when(orderRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByOrderId(entity.getId())).thenReturn(Optional.of(new PaymentEntity()));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service().handleMercadoPagoWebhook("555");

        assertThat(entity.getStatus()).isEqualTo("confirmado");
        assertThat(entity.getPaymentStatus()).isEqualTo("completado");
    }
}
