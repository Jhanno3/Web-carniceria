# Tasks 003 — Mercado Pago

- [x] T1. `com.mercadopago:sdk-java:2.5.0` agregado al `pom.xml` — compiló al primer intento.
- [x] T2. [P] Test unitario: `Order.confirmMercadoPagoPayment`/`rejectMercadoPagoPayment` — transición válida, idempotencia al confirmar dos veces, rechazo si el método no es `mercadopago`. `OrderTest` ahora tiene 17 tests.
- [x] T3. Transiciones implementadas en `model.Order`.
- [x] T4. `service.MercadoPagoService`: `createPreference(Order)` y `getPayment(String id)`.
- [x] T5. [P] `OrderServiceTest`: `createOrder` con `mercadopago` pide la preferencia y devuelve el `initPoint`; webhook ignora un pedido inexistente; confirma el pedido correcto con un pago `approved`. 10/10 en verde.
- [x] T6. `OrderService` extendido (`createOrder` devuelve `OrderCreationResult(order, mercadopagoInitPoint)`; nuevo `handleMercadoPagoWebhook`). `mercadopagoInitPoint` sumado a `OrderResponseDto`/`OrderMapper`.
- [x] T7. `controller.WebhookController` (`POST /api/webhooks/mercadopago`, público) + `SecurityConfig.permitAll()` en `/api/webhooks/**`.
- [x] T8. `MercadoPagoWebhookIT` — **4/4 en verde**: init point real de la preferencia, webhook `approved` confirma el pedido correcto, referencia desconocida no rompe nada, webhook sin auth responde igual (es público).
- [x] T9. `app.base-url` (nueva) y `app.cors-allowed-origins` (ya existía) se usan para `notificationUrl`/`backUrls` en `MercadoPagoService`. `MERCADOPAGO_ACCESS_TOKEN` ya estaba en `backend/.env.local`.
- [x] T10. "Mercado Pago" habilitado en `checkout.html` (ahora es la opción por defecto). `checkout.ts` redirige el navegador a `order.mercadopagoInitPoint` cuando la respuesta lo trae, en vez de mostrar la confirmación directo. `CreateOrderRequest`/`OrderResponse` en `api.ts` actualizados.
- [x] T11. Probado contra el backend real + API real de Mercado Pago (ambiente test, no mocks): `POST /api/orders` con `mercadopago` devolvió un `init_point` real (`mercadopago.com.ar/checkout/v1/redirect?pref_id=...`), con una preferencia de verdad creada del lado de Mercado Pago. `POST /api/webhooks/mercadopago` confirmado público (sin JWT) y resiliente (un `payment_id` que no existe no rompe nada, responde 200 igual) contra el backend corriendo de verdad.

## Cierre

- [x] T12. Cobertura por historia de `spec.md`:
  - US1 (elegir MP y pagar): `checkout.ts` + probado manualmente con una preferencia real.
  - US2 (vuelta confirmada sin hacer nada): cubierto por el webhook (`MercadoPagoWebhookIT` + prueba manual del endpoint público).
  - US3 (admin se entera sin intervenir): mismo mecanismo de notificación que efectivo/transferencia (`notify(..., "mercadopago_confirmado")`), visible en `admin.html`.
  - US4 (webhook no falsificable): `MercadoPagoService.getPayment` siempre vuelve a consultar la API real antes de confirmar nada — nunca se usa el `status` que venga en el body del webhook.
