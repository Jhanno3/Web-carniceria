package com.carniceria.backend.service;

import com.carniceria.backend.dto.request.CreateOrderRequestDto;
import com.carniceria.backend.dto.request.OrderItemRequestDto;
import com.carniceria.backend.entity.*;
import com.carniceria.backend.exception.ForbiddenException;
import com.carniceria.backend.exception.OrderNotFoundException;
import com.carniceria.backend.exception.ProfileNotFoundException;
import com.carniceria.backend.mapper.OrderMapper;
import com.carniceria.backend.model.*;
import com.carniceria.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProfileRepository profileRepository;
    private final PaymentRepository paymentRepository;
    private final AdminNotificationRepository notificationRepository;
    private final MercadoPagoService mercadoPagoService;
    private final OrderMapper mapper;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                         ProfileRepository profileRepository, PaymentRepository paymentRepository,
                         AdminNotificationRepository notificationRepository, MercadoPagoService mercadoPagoService,
                         OrderMapper mapper) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.profileRepository = profileRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.mapper = mapper;
    }

    public record OrderCreationResult(Order order, String mercadopagoInitPoint) {
    }

    @Transactional
    public OrderCreationResult createOrder(UUID userId, CreateOrderRequestDto dto) {
        requireCompleteProfile(userId);

        PaymentMethod paymentMethod = PaymentMethod.valueOf(dto.paymentMethod().toUpperCase());
        DeliveryType deliveryType = DeliveryType.valueOf(dto.deliveryType().toUpperCase());
        if (deliveryType == DeliveryType.ENVIO && isBlank(dto.addressText())) {
            throw new IllegalArgumentException("Falta la dirección de envío.");
        }

        List<OrderItem> items = dto.items().stream().map(this::toOrderItem).toList();
        Order order = Order.create(UUID.randomUUID(), userId, paymentMethod, deliveryType,
                dto.addressText(), dto.addressLat(), dto.addressLng(), dto.notes(), items, Instant.now());

        orderRepository.save(mapper.toEntity(order));
        savePayment(order);

        String initPoint = null;
        if (paymentMethod == PaymentMethod.MERCADOPAGO) {
            MercadoPagoService.PreferenceResult preference = mercadoPagoService.createPreference(order);
            initPoint = preference.initPoint();
            paymentRepository.findByOrderId(order.id()).ifPresent(payment -> {
                payment.setMercadopagoPreferenceId(preference.preferenceId());
                paymentRepository.save(payment);
            });
        }

        notify(order.id(), notificationTypeFor(paymentMethod));
        return new OrderCreationResult(order, initPoint);
    }

    @Transactional
    public Order markTransferSent(UUID userId, UUID orderId) {
        OrderEntity entity = findEntity(orderId);
        if (!entity.getUserId().equals(userId)) {
            throw new ForbiddenException("No podés modificar el pedido de otro usuario.");
        }
        Order updated = mapper.toModel(entity).markTransferSent();
        applyStatus(entity, updated);
        notify(orderId, "transferencia_marcada");
        return updated;
    }

    @Transactional
    public Order confirmTransferPayment(Role requesterRole, UUID orderId) {
        requireAdmin(requesterRole);
        OrderEntity entity = findEntity(orderId);
        Order updated = mapper.toModel(entity).confirmTransferPayment();
        applyStatus(entity, updated);
        markPaymentCompleted(orderId);
        return updated;
    }

    /**
     * Nunca confía en el cuerpo del webhook: vuelve a pedirle el pago entero a la API de Mercado
     * Pago y recién ahí actualiza algo (ver plan.md de specs/003-mercadopago).
     */
    @Transactional
    public void handleMercadoPagoWebhook(String mercadopagoPaymentId) {
        MercadoPagoService.MercadoPagoPaymentInfo payment = mercadoPagoService.getPayment(mercadopagoPaymentId);
        if (payment.externalReference() == null) {
            return;
        }
        UUID orderId = parseUuidOrNull(payment.externalReference());
        if (orderId == null) {
            return;
        }
        Optional<OrderEntity> maybeEntity = orderRepository.findById(orderId);
        if (maybeEntity.isEmpty()) {
            return;
        }
        OrderEntity entity = maybeEntity.get();
        Order current = mapper.toModel(entity);
        if (current.paymentMethod() != PaymentMethod.MERCADOPAGO) {
            return;
        }

        Order updated = switch (payment.status()) {
            case "approved" -> current.confirmMercadoPagoPayment();
            case "rejected", "cancelled" -> current.rejectMercadoPagoPayment();
            default -> null; // pending, in_process, etc.: no hacemos nada todavía
        };
        if (updated == null) {
            return;
        }

        applyStatus(entity, updated);
        paymentRepository.findByOrderId(orderId).ifPresent(p -> {
            p.setMercadopagoPaymentId(payment.id());
            p.setStatus(updated.paymentStatus().name().toLowerCase());
            if (updated.paymentStatus() == PaymentStatus.COMPLETADO) {
                p.setConfirmedAt(Instant.now());
            }
            paymentRepository.save(p);
        });
        if (updated.paymentStatus() == PaymentStatus.COMPLETADO) {
            notify(orderId, "mercadopago_confirmado");
        }
    }

    public Order getOrderById(UUID requesterId, Role requesterRole, UUID orderId) {
        OrderEntity entity = findEntity(orderId);
        if (requesterRole != Role.ADMIN && !entity.getUserId().equals(requesterId)) {
            throw new ForbiddenException("No podés ver el pedido de otro usuario.");
        }
        return mapper.toModel(entity);
    }

    public List<Order> listAllOrders(Role requesterRole) {
        requireAdmin(requesterRole);
        return orderRepository.findAll().stream().map(mapper::toModel).toList();
    }

    public ProfileEntity profileOf(UUID userId) {
        return profileRepository.findById(userId).orElseThrow(() -> new ProfileNotFoundException(userId));
    }

    private void requireCompleteProfile(UUID userId) {
        ProfileEntity profile = profileOf(userId);
        if (isBlank(profile.getFirstName()) || isBlank(profile.getLastName()) || isBlank(profile.getPhone())) {
            throw new IllegalStateException("Completá tu perfil (nombre, apellido y teléfono) antes de confirmar un pedido.");
        }
    }

    private OrderItem toOrderItem(OrderItemRequestDto itemDto) {
        ProductEntity product = productRepository.findById(itemDto.productId())
                .filter(ProductEntity::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + itemDto.productId()));
        Unit unit = Unit.valueOf(product.getUnit().toUpperCase());
        return OrderItem.of(product.getId(), product.getName(), unit, itemDto.quantity(),
                product.getPricePerUnit(), itemDto.itemNotes());
    }

    private void savePayment(Order order) {
        PaymentEntity payment = new PaymentEntity();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(order.id());
        payment.setMethod(order.paymentMethod().name().toLowerCase());
        payment.setStatus(order.paymentStatus().name().toLowerCase());
        payment.setAmount(order.totalEstimated());
        payment.setCreatedAt(Instant.now());
        paymentRepository.save(payment);
    }

    private void markPaymentCompleted(UUID orderId) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus("completado");
            payment.setConfirmedAt(Instant.now());
            paymentRepository.save(payment);
        });
    }

    private void notify(UUID orderId, String type) {
        AdminNotificationEntity notification = new AdminNotificationEntity();
        notification.setId(UUID.randomUUID());
        notification.setOrderId(orderId);
        notification.setType(type);
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
    }

    /** Actualiza solo los campos de estado sobre la misma entidad cargada: nunca toca la colección de ítems. */
    private void applyStatus(OrderEntity entity, Order updated) {
        entity.setStatus(updated.status().name().toLowerCase());
        entity.setPaymentStatus(updated.paymentStatus().name().toLowerCase());
        entity.setUpdatedAt(updated.updatedAt());
        orderRepository.save(entity);
    }

    private OrderEntity findEntity(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void requireAdmin(Role role) {
        if (role != Role.ADMIN) {
            throw new ForbiddenException("Solo el admin puede hacer esto.");
        }
    }

    private String notificationTypeFor(PaymentMethod method) {
        return switch (method) {
            case EFECTIVO -> "efectivo_pendiente";
            case MERCADOPAGO -> "nuevo_pedido";
            case TRANSFERENCIA -> "nuevo_pedido";
        };
    }

    private UUID parseUuidOrNull(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
