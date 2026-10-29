# Plan 003 — Mercado Pago (Checkout Pro)

## Resumen técnico

Se usa **Checkout Pro** (redirect a una página hospedada por Mercado Pago), vía el SDK oficial `com.mercadopago:sdk-java:2.5.0`. El backend crea una *Preference* con los ítems del pedido, Mercado Pago aloja el cobro, y nos avisa por **webhook** cuando cambia el estado del pago — nunca confiamos en el navegador del cliente para eso (podría cerrar la pestaña antes de volver).

## Capas nuevas/tocadas

| Capa | Cambios |
|---|---|
| `model` | `Order.confirmMercadoPagoPayment(String mpPaymentId)` y `Order.rejectMercadoPagoPayment()` — mismo patrón que las transiciones de transferencia. `confirmMercadoPagoPayment` es **idempotente**: si el pedido ya está `confirmado`, no hace nada (Mercado Pago puede mandar el mismo webhook más de una vez). |
| `service` | `MercadoPagoService` (nuevo): `createPreference(Order order)` → `{preferenceId, initPoint}`; `getPayment(String paymentId)` → consulta real a la API de MP. `OrderService.createOrder` ya no rechaza `mercadopago`: si es ese método, pide la preferencia después de guardar el pedido y la devuelve junto con el pedido. `OrderService.handleMercadoPagoWebhook(String paymentId)`: pide el pago real a `MercadoPagoService`, busca el pedido por `externalReference`, aplica la transición según el estado (`approved` → confirmar, `rejected`/`cancelled` → rechazar, cualquier otro estado → no hacer nada), y si no encuentra el pedido, no hace nada (200 igual, para no generar reintentos de MP). |
| `dto.response` | `OrderResponseDto` suma `mercadopagoInitPoint` (nullable, solo se completa si el método es `mercadopago`). |
| `controller` | `WebhookController` nuevo: `POST /api/webhooks/mercadopago`, **sin autenticación** (JWT no aplica acá). Lee `type`/`data.id` del body. |
| `config` | `SecurityConfig`: `permitAll()` en `/api/webhooks/**` antes del `anyRequest().authenticated()`. Nueva property `app.mercadopago-access-token`. |

## Detalle de la preferencia

- `items`: uno por cada `OrderItem`, `title` = nombre del producto, `quantity`/`unitPrice` según corresponda (OJO: la API de MP pide `quantity` entero — para ítems por kg con decimales, se manda `quantity=1` y `unitPrice=subtotal` del ítem, porque MP no soporta cantidades fraccionarias).
- `externalReference` = `order.id()` (así el webhook puede encontrar el pedido).
- `notificationUrl` = `${APP_BASE_URL}/api/webhooks/mercadopago` (pública; en local no es alcanzable por Mercado Pago salvo con un túnel — se simula el webhook a mano con `curl` para probarlo, igual que ya se hace con los otros flujos).
- `backUrls` (success/failure/pending) = `${CORS_ALLOWED_ORIGINS}/checkout.html` (reusa la misma variable que ya configura el origen del frontend).

## Webhook — por qué es seguro

Mercado Pago manda `{"type":"payment","data":{"id":"123"}}` (u otros tipos que ignoramos). El backend **nunca** usa el estado que venga en ese body: con el `id` vuelve a pedirle el pago entero a la API de Mercado Pago (`PaymentClient.get(id)`, autenticado con nuestro Access Token) y recién ahí confía en el `status` y el `externalReference` que la propia API de MP devuelve. Alguien que mande un POST falso a `/api/webhooks/mercadopago` con un id que no existe o no es suyo no logra nada: la consulta a la API real es la que manda.

## Tests

- Unitarios: `Order` — confirmar/rechazar pago de Mercado Pago, idempotencia de confirmar dos veces. `OrderServiceTest` — `createOrder` con `mercadopago` llama a `MercadoPagoService.createPreference` y devuelve el `initPoint`; `handleMercadoPagoWebhook` ignora un `externalReference` que no corresponde a ningún pedido; confirma el pedido correcto cuando el estado es `approved`.
- End-to-end (`MercadoPagoWebhookIT`): como no se puede pegarle a la API real de Mercado Pago desde un test (necesitaría un pago de verdad), `MercadoPagoService` se mockea en este `IT` puntual (es la única vez que un `IT` mockea algo externo — la frontera del sistema es la API de Mercado Pago, no nuestra base). Prueba: pedido con `mercadopago` → webhook con pago `approved` → pedido `confirmado`/`completado`; webhook con `externalReference` desconocido no rompe nada.
- Prueba manual: crear un pedido real por Mercado Pago contra el ambiente de test, pagar con una tarjeta de prueba, y simular el webhook a mano con `curl` (ya que no es alcanzable desde afuera en local) para confirmar que el pedido pasa a confirmado.

## Abierto

- `APP_BASE_URL` en producción (Railway) tiene que ser la URL pública real para que el webhook funcione — en local se simula a mano.
- No se implementan reembolsos ni pagos parciales.
