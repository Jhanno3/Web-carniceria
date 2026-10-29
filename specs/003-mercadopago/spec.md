# Spec 003 — Pago con Mercado Pago

Construye sobre [specs/002-pedidos-y-pagos](../002-pedidos-y-pagos/spec.md). Agrega el tercer método de pago que quedó explícitamente afuera ahí.

## Por qué

Transferencia y efectivo ya funcionan. Mercado Pago es el método que más fricción saca (cobro con tarjeta, en el momento) y es lo único que falta para que el checkout esté completo.

## Alcance

- Cliente elige "Mercado Pago" en el checkout → se crea el pedido (igual que los otros métodos) y se lo redirige al checkout hospedado de Mercado Pago (Checkout Pro) a pagar.
- Mercado Pago le pega a un webhook nuestro cuando el pago se acredita (o falla) → el pedido pasa a `confirmado`/`completado` (o `rechazado`) automáticamente, sin que un humano tenga que hacer nada.
- El admin se entera igual que con los otros métodos (vía `admin.html`).
- El cliente, al volver del checkout de Mercado Pago, ve una pantalla de confirmación.

No incluye: reembolsos, pagos parciales, otros medios dentro de Mercado Pago (Checkout API transparente con tarjeta embebida) — se usa **Checkout Pro** (redirect), que es la integración más simple y la que corresponde a "generar un cobro" como dice `constitution.md`.

## Historias de usuario

**US1.** Como cliente, elijo Mercado Pago, confirmo mi pedido, y voy al checkout de Mercado Pago a pagar.

**US2.** Como cliente, después de pagar, vuelvo al sitio y veo que mi pedido quedó confirmado (sin tener que hacer nada más).

**US3.** Como admin, cuando un pago de Mercado Pago se acredita, me entero en `admin.html` igual que con los otros métodos, sin intervenir.

**US4.** El webhook nunca puede ser falsificado: antes de dar un pedido por pagado, el backend verifica el pago contra la propia API de Mercado Pago (nunca confía en el cuerpo de la notificación a ciegas).

## Restricciones heredadas

- SDK oficial de Java (`com.mercadopago:sdk-java`), nunca llamadas HTTP a mano a la API de MP.
- Credenciales de **TEST** mientras se desarrolla (ya las tenemos en `backend/.env.local`).
- Capas y tests obligatorios de `constitution.md` secciones 7 y 9.

## Cómo se valida

- Un pedido con Mercado Pago se puede crear y devuelve una URL de pago real de Mercado Pago (ambiente test).
- Un webhook de pago aprobado simulado contra el backend deja el pedido `confirmado`/`completado`.
- Un webhook con un `payment_id` que no corresponde a ningún pedido nuestro no rompe nada (se ignora).
- Todo lo anterior con al menos un test automatizado.
