# Plan 002 — Pedidos, efectivo y transferencia

Traduce `spec.md` de esta carpeta a diseño técnico, sobre la arquitectura y el stack de [constitution.md](../../agents/constitution.md).

## Prerrequisito: tabla `products`

El carrito hoy vive solo en el frontend (`PRODUCTS` hardcodeado en `index.html`, con los 45 cortes reales + precios). Para crear un pedido **no podemos confiar en el precio que mande el cliente** (alguien podría mandar `price: 1` manipulando el request) — el backend necesita su propia fuente de verdad. Por eso esta spec agrega la tabla `products` (ya prevista en `constitution.md` sección 3, roadmap paso 5) como prerrequisito, sembrada con los mismos ids/nombres/precios que ya están en `index.html`, para que el `OrderService` calcule el precio real en el servidor, ignorando lo que mande el cliente salvo `productId`, `quantity` y `itemNotes`.

No se expone un `GET /api/products` público en esta spec — el catálogo del frontend sigue siendo la fuente visual (eso es el roadmap paso 6, aparte). La tabla `products` acá es de uso interno del backend para validar pedidos.

## Modelo de datos (SQL nuevo, en `supabase/migrations/0002_orders.sql`)

Tablas: `products`, `orders`, `order_items`, `payments`, `admin_notifications`, tal como están definidas en `constitution.md` sección 3, con RLS (cliente ve lo suyo, admin ve todo) — igual patrón que `profiles`.

**Importante:** el backend Java se conecta a Postgres con un usuario propio (no pasa por PostgREST), así que **RLS no es lo que lo protege** — eso ya lo vimos en feature 001: la autorización real vive en la capa `service` (igual que `ProfileService`). RLS acá es defensa en profundidad para el día que algo (ej. Supabase Realtime en `admin_notifications`) consulte estas tablas directo con el SDK de Supabase.

## Estados (coinciden con constitution.md sección 3)

- `orders.status`: `pendiente_pago` → (`pago_en_revision` si es transferencia) → `confirmado` → ... | `cancelado`.
- `orders.payment_status`: `pendiente` → `completado` | `rechazado`.
- Efectivo: se crea con `status=confirmado`, `payment_status=pendiente` (se cobra al entregar).
- Transferencia: se crea con `status=pendiente_pago`, `payment_status=pendiente`. Al marcar "Ya transferí": `status=pago_en_revision` (el `payment_status` NO cambia solo porque el cliente lo diga). El admin, tras verificar a mano, marca `payment_status=completado` y `status=confirmado`.

## Capas del backend (paquetes nuevos, mismo patrón que `profile`)

| Capa | Clases |
|---|---|
| `entity` | `ProductEntity`, `OrderEntity`, `OrderItemEntity`, `PaymentEntity`, `AdminNotificationEntity` |
| `model` | `Order`, `OrderItem`, `Payment`, y los enums `OrderStatus`, `PaymentMethod`, `PaymentStatus`, `DeliveryType`. Reglas: un pedido necesita ≥1 ítem; "marcar transferencia enviada" solo es válido si `paymentMethod=TRANSFERENCIA` y `status=PENDIENTE_PAGO`; el total se calcula sumando los ítems, nunca se acepta del cliente. |
| `dto.request` | `CreateOrderRequestDto` (`deliveryType`, `addressText`/`Lat`/`Lng` opcionales, `paymentMethod`, `items: [{productId, quantity, itemNotes}]`) — **sin precio ni nombre**, eso sale de `products`. |
| `dto.response` | `OrderResponseDto` (con sus items), `AdminOrderResponseDto` (incluye datos del cliente, solo para el admin) |
| `repository` | `ProductRepository`, `OrderRepository`, `OrderItemRepository`, `PaymentRepository`, `AdminNotificationRepository` |
| `service` | `OrderService` (`createOrder`, `markTransferSent`, `confirmTransferPayment` (admin), `getMyOrders`, `getOrderById`, `listAllOrders` (admin)) |
| `controller` | `OrderController` (`POST /api/orders`, `GET /api/orders/{id}`, `PUT /api/orders/{id}/mark-transfer-sent`), `AdminOrderController` (`GET /api/admin/orders`, `PUT /api/admin/orders/{id}/confirm-transfer`) |
| `mapper` | `OrderMapper` |

`OrderService.createOrder`: por cada item del request, busca el `ProductEntity` por id (si no existe o está inactivo, rechaza), arma un `OrderItem` con el precio y nombre **de la tabla, no del request**, calcula subtotal, arma el `Order` con sus items, guarda todo en una transacción, y crea el `AdminNotification` correspondiente (`nuevo_pedido`).

## Endpoints

| Método | Path | Quién | Qué hace |
|---|---|---|---|
| `POST` | `/api/orders` | cliente autenticado con perfil completo | Crea el pedido desde el carrito. |
| `GET` | `/api/orders/{id}` | el dueño del pedido, o admin | Detalle de un pedido. |
| `PUT` | `/api/orders/{id}/mark-transfer-sent` | el dueño del pedido | Marca "Ya transferí" (solo si el método es transferencia y sigue `pendiente_pago`). |
| `GET` | `/api/admin/orders` | solo admin | Lista todos los pedidos, con datos del cliente. |
| `PUT` | `/api/admin/orders/{id}/confirm-transfer` | solo admin | Marca la transferencia como verificada: `payment_status=completado`, `status=confirmado`. |

## Frontend

- `checkout.ts`: el botón de confirmar ahora sí llama a `POST /api/orders` con los items del carrito (leídos del mismo snapshot que ya arma `index.html`, pero ahora sin precio, solo `productId`/`quantity`/`itemNotes`), espera la respuesta, vacía el carrito (`localStorage`) y muestra una pantalla de confirmación. Si el perfil está incompleto, redirige a `/perfil.html` antes de dejar confirmar.
- Para transferencia: botón "Ya transferí" que llama al endpoint correspondiente y actualiza el estado en pantalla.
- `admin.html` + `admin.ts` (nueva página, mínima): protegida por rol (si `role !== admin`, redirige a `/`), tabla simple con los pedidos (`GET /api/admin/orders`) y un botón "Marcar transferencia verificada" en los que estén `pago_en_revision`. Sin filtros, sin paginación — eso es la spec del panel admin completo, más adelante.

## Tests (sección 9 de la constitución)

- Unitarios: `OrderTest` (reglas: pedido sin ítems rechazado; "marcar transferencia" rechazado si no es transferencia o si ya no está `pendiente_pago`); `OrderMapperTest`; `OrderServiceTest` (repos mockeados: `createOrder` ignora el precio del request y usa el de `products`; cliente no puede ver/marcar el pedido de otro; `listAllOrders`/`confirmTransfer` rechazan a quien no es admin).
- End-to-end (`OrderFlowIT`, Testcontainers): crear pedido en efectivo (camino feliz) → `status=confirmado`; crear pedido por transferencia → marcar "ya transferí" → `status=pago_en_revision` → admin confirma → `status=confirmado`, `payment_status=completado`; un cliente intentando ver/marcar el pedido de otro falla; un cliente (no admin) pegándole a `/api/admin/orders` falla.

## Abierto

- Dirección de envío: en esta spec se persiste como texto/lat/lng sueltos en el pedido (copiados del perfil o editados en el checkout), sin validación de zona ni costo de envío — eso es roadmap aparte.
- `admin.html` es deliberadamente mínimo; el panel admin "de verdad" (filtros, estados intermedios como `preparando`/`en_camino`/`entregado`, ver historial de un cliente) es una spec futura.
