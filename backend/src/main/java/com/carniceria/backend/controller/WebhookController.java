package com.carniceria.backend.controller;

import com.carniceria.backend.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Público, sin JWT (ver SecurityConfig). Mercado Pago nos pega acá, nunca al revés.
 * Siempre responde 200 rápido — nunca confía en el cuerpo, solo lo usa para saber a qué pago
 * volver a preguntarle a la API real (ver OrderService.handleMercadoPagoWebhook).
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final OrderService orderService;

    public WebhookController(OrderService orderService) {
        this.orderService = orderService;
    }

    public record MercadoPagoDataDto(String id) {
    }

    public record MercadoPagoNotificationDto(String type, String topic, MercadoPagoDataDto data) {
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> handleMercadoPago(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id,
            @RequestBody(required = false) MercadoPagoNotificationDto body) {

        String type = body != null && body.type() != null ? body.type() : topic;
        String paymentId = body != null && body.data() != null ? body.data().id() : id;

        try {
            if ("payment".equals(type) && paymentId != null) {
                orderService.handleMercadoPagoWebhook(paymentId);
            }
        } catch (Exception ignored) {
            // Nunca le devolvemos un error a Mercado Pago por esto: a lo sumo reintenta el webhook,
            // y si el pago era real, el admin lo va a ver de todas formas en /admin.html.
        }
        return ResponseEntity.ok().build();
    }
}
