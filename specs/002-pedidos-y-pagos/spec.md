# Spec 002 — Pedidos, efectivo y transferencia

Estado: borrador para revisión. Segunda feature del flujo SDD, construye sobre [specs/001-autenticacion-y-perfil](../001-autenticacion-y-perfil/spec.md) (requiere login y perfil).

## Por qué

El carrito ya funciona (feature frontend previa) y `checkout.html` ya muestra un resumen, pero no crea ningún pedido real ni avisa a nadie. Esta spec cierra ese círculo: el cliente confirma su pedido, elige cómo paga, y el admin se entera.

## Alcance de esta spec

Incluye:
- Crear un pedido real (con sus ítems) a partir del carrito, al confirmar en el checkout.
- Elegir método de pago: **efectivo** o **transferencia** (Mercado Pago queda para una spec futura — necesita integrar su API y es más grande, ver "Fuera de alcance").
- Para transferencia: mostrar los datos de la cuenta y que el cliente marque "Ya transferí".
- Que el admin se entere de cada pedido nuevo (notificación mínima, no un panel completo).
- Pantalla de confirmación para el cliente al terminar.

No incluye (quedan para specs futuras):
- Pago con Mercado Pago (API, preferencia, webhook).
- Panel admin completo (filtros, cambiar estado de un pedido, ver historial de un cliente).
- "Mis pedidos" del cliente (historial propio, más allá de la confirmación inmediata).
- Selector de presentación por corte (grosor, con/sin hueso).
- Costo de envío / validación de zona.

## Actores

- **Cliente** (`role = client`): tiene un carrito con al menos un producto, está logueado y con perfil completo (nombre, apellido, teléfono; dirección si elige envío).
- **Administrador** (`role = admin`): se entera de cada pedido nuevo y de cada transferencia marcada.

## Historias de usuario y criterios de aceptación

**US1 — Confirmar pedido.** Como cliente con productos en el carrito, quiero confirmar mi pedido para que se registre de verdad.
- Si mi perfil está incompleto (falta nombre, apellido o teléfono), no puedo confirmar: me lleva a completar el perfil primero.
- Al confirmar, se crea un pedido con todos los ítems del carrito: producto, cantidad (kg o unidades), precio al momento del pedido, nota de ese ítem si puse una, y el total estimado.
- El carrito se vacía después de confirmar.
- Veo una pantalla de confirmación con el número/resumen del pedido.

**US2 — Elegir cómo entrego mis datos de envío.** Como cliente, al confirmar elijo envío a domicilio o retiro en el local.
- Si elijo envío, el pedido guarda la dirección (la del perfil, o la que haya cargado para ese pedido).
- Si elijo retiro, no hace falta dirección.

**US3 — Pagar en efectivo.** Como cliente, elijo pagar en efectivo al recibir.
- El pedido queda confirmado de inmediato, con el pago marcado como pendiente.
- El admin se entera de este pedido nuevo.

**US4 — Pagar por transferencia.** Como cliente, elijo transferencia.
- Veo los datos de la cuenta para transferir.
- Tengo un botón "Ya transferí" que marca mi pedido como "a confirmar" — no se da por pagado automáticamente, porque nadie lo verificó todavía.
- El admin se entera de que hay una transferencia para revisar.

**US5 — El admin se entera.** Como admin, cuando entra un pedido nuevo (cualquier método) o alguien marca "Ya transferí", quiero verlo.
- Para esta spec alcanza con poder consultarlo (lista simple de pedidos/notificaciones); no hace falta un panel pulido todavía.
- Un cliente nunca puede ver esta información de otro cliente, ni acceder a la vista de notificaciones del admin.

## Restricciones heredadas de `constitution.md`

- Modelo de datos: `orders`, `order_items`, `payments`, `admin_notifications` (sección 3).
- Flujo de compra y de cada método de pago (sección 4).
- Arquitectura en capas del backend y tests obligatorios (secciones 7 y 9).
- Transferencia: los datos de la cuenta son placeholder (`[CBU]`, `[ALIAS]`, `[TITULAR]`) hasta tener los reales — ya están así en `checkout.html`.

## Cómo se valida esta feature (criterios de cierre)

- Un cliente logueado, con perfil completo y productos en el carrito, puede confirmar un pedido por efectivo o por transferencia y ver la confirmación.
- El pedido y sus ítems quedan guardados correctamente (cantidades, precios y notas coinciden con lo que había en el carrito).
- El admin puede consultar que el pedido nuevo existe.
- Un cliente no puede ver ni modificar pedidos de otro cliente (test automatizado, como en feature 001).
- Todo lo anterior tiene al menos un test automatizado.

## Fuera de alcance (backlog para specs futuras)

- Mercado Pago (spec 003).
- Panel admin completo (spec 004, probablemente junto con "Mis pedidos" del cliente).
