# Tasks 002 — Pedidos, efectivo y transferencia

Deriva de `plan.md`. `[P]` = en paralelo con la tarea anterior. Tests antes que la implementación que los hace pasar.

## Supabase

- [x] T1. `supabase/migrations/0002_orders.sql`: tablas `products`, `orders`, `order_items`, `payments`, `admin_notifications` + RLS — aplicado contra la base real.
- [x] T2. Seed de `products`: 53 filas (45 cortes + 8 almacén) insertadas, generadas directo del `PRODUCTS` de `index.html` para que los ids coincidan exacto.

## Backend — capa `model`

- [x] T3. [P] Test unitario `OrderTest` + `PaymentTest`: pedido sin ítems se rechaza; "marcar transferencia enviada" se rechaza si el método no es transferencia o si el pedido ya no está `pendiente_pago`; el total se calcula sumando los ítems; el pago no puede ser negativo (sí puede ser $0, mientras el catálogo de almacén tenga precios placeholder).
- [x] T4. `model.Order`, `model.OrderItem`, `model.Payment` y los enums — 14 tests en verde.

## Backend — capa `entity` + `mapper`

- [x] T5. [P] Test unitario `OrderMapperTest`: `entity → model` y `model → dto.response` de un pedido con sus ítems.
- [x] T6. `entity.ProductEntity`, `entity.OrderEntity` (con `OneToMany` cascade a sus ítems), `entity.OrderItemEntity`, `entity.PaymentEntity`, `entity.AdminNotificationEntity` y `mapper.OrderMapper` — 3 tests en verde. (No se creó un `OrderItemRepository` aparte: los ítems se persisten en cascada a través de `OrderRepository`, no hacía falta.)

## Backend — capa `repository` + `service`

- [x] T7. [P] Test unitario `OrderServiceTest` (repos mockeados, 7 casos): `createOrder` usa el precio de `products` (el DTO ni siquiera acepta un precio del cliente, así que no hay forma de mandar uno manipulado); rechaza un `productId` inexistente; rechaza perfil incompleto; rechaza envío sin dirección; `markTransferSent` rechaza a quien no es el dueño; `listAllOrders`/`confirmTransferPayment` rechazan a quien no es admin.
- [x] T8. `repository.ProductRepository`/`OrderRepository`/`PaymentRepository`/`AdminNotificationRepository` y `service.OrderService` — 7/7 tests en verde.

## Backend — capa `dto` + `controller`

- [x] T9. `dto.request.CreateOrderRequestDto`/`OrderItemRequestDto` y `dto.response.OrderResponseDto`/`OrderItemResponseDto`/`AdminOrderResponseDto`.
- [x] T10. `controller.OrderController` (`POST /api/orders`, `GET /api/orders/{id}`, `PUT /api/orders/{id}/mark-transfer-sent`) y `controller.AdminOrderController` (`GET /api/admin/orders`, `PUT /api/admin/orders/{id}/confirm-transfer`). Se extrajo `security.CurrentUser` (compartido con `ProfileController`) para resolver `userId`/`role` desde el JWT, ya que ahora dos controllers lo necesitaban igual.

## Backend — end-to-end

- [x] T11. `OrderFlowIT` — **4/4 en verde**: pedido en efectivo → `confirmado`; pedido por transferencia → "ya transferí" → `pago_en_revision` → admin confirma → `confirmado`/`completado`; cliente A no puede ver ni marcar el pedido de cliente B; cliente (no admin) rechazado en `/api/admin/orders`.
- [x] T12. Corre como `IT` vía `mvn verify` (failsafe), separado de los 41 unitarios de `mvn test`.

## Frontend

- [x] T13. `checkout.ts` reescrito: snapshot del carrito ahora lleva `productId`/`quantity` reales (antes solo el texto formateado). El botón "Confirmar pedido" llama a `POST /api/orders`, vacía `localStorage.cart` y `sessionStorage.checkoutSnapshot`, y muestra la pantalla de confirmación. Si falta la dirección para envío, se bloquea antes de pegarle a la API con un mensaje claro.
- [x] T14. Botón "Ya transferí" en la pantalla de confirmación (solo visible si el método fue transferencia), llama a `PUT /api/orders/{id}/mark-transfer-sent` y pasa a "Gracias, te avisamos apenas verifiquemos la transferencia."
- [x] T15. `admin.html` + `src/pages/admin.ts`: redirige a `/` si el que entra no es admin, lista `GET /api/admin/orders` (nombre/teléfono/dirección del cliente, ítems, estado) y tiene botón "Marcar transferencia verificada" en los pedidos `pago_en_revision`.
- [x] T16. Probado a mano con `curl` contra el backend real + Supabase real: pedido en efectivo (confirmado al toque) y pedido por transferencia (marcar → confirmar admin → `confirmado`/`completado`) — los dos de punta a punta, sin mocks.

## Cierre

- [x] T17. Cobertura por criterio de `spec.md`:
  - US1 (confirmar pedido): `OrderTest`, `OrderServiceTest`, `OrderFlowIT` + probado manualmente real.
  - US2 (envío/retiro): validado en `OrderService` (rechaza envío sin dirección) + `checkout.ts`.
  - US3 (efectivo): `OrderFlowIT` camino feliz + probado manualmente real.
  - US4 (transferencia): `OrderFlowIT` completo (marcar + confirmar) + probado manualmente real.
  - US5 (admin se entera): `admin.html` lista los pedidos; `AdminOrderController`/`OrderService` rechazan a quien no es admin, cubierto en `OrderServiceTest` y `OrderFlowIT`.
  - Mercado Pago: explícitamente rechazado en `OrderService.parsePaymentMethod` (400) y sacado del selector del checkout — no quedó a medio hacer, es la próxima spec (003).
  - Panel admin completo (filtros, más estados, historial por cliente): no se tocó, sigue siendo backlog.
