# constitution.md — Arquitectura técnica del proyecto

Define el stack, el modelo de datos, los roles y los flujos de pago/notificación para la tienda online de carnicería y almacén. [AGENTS.md](./AGENTS.md) sigue siendo la autoridad en diseño visual, tono y componentes del frontend; este documento es la autoridad en arquitectura, datos, backend, auth y pagos. Ante un conflicto: diseño visual → manda AGENTS.md; datos/backend/seguridad → manda este archivo.

Leer entero antes de tocar backend, base de datos o checkout.

## 1. Stack tecnológico (decisión final, no re-discutir sin motivo fuerte)

| Capa | Tecnología |
|---|---|
| Base de datos | Supabase (Postgres) |
| Autenticación | Supabase Auth (email + password), login y registro |
| Backend | Java 21, Spring Boot 4, Maven |
| Frontend | Tailwind CSS + TypeScript (migrando desde `frontend/index.html`) |
| Pagos | MercadoPago (API de cobros), transferencia bancaria manual, efectivo |

El `index.html` actual sigue siendo la fuente de verdad del diseño y contenido mientras se migra. No se reinventa la landing: se porta tal cual a Tailwind, traduciendo los tokens de `:root` (`--accent`, `--ink`, `--muted`, etc.) a `theme.extend.colors` / `fontFamily` / `borderRadius` en `tailwind.config`.

## 2. Roles de cuenta

Dos roles únicamente:

- **`client`** — cualquiera que se registra desde la web. Solo ve y gestiona sus propios datos y pedidos.
- **`admin`** — **una sola cuenta**, la del dueño del negocio. No hay flujo de alta de admin desde la web; se provisiona a mano (directo en Supabase o con un script de seed) una sola vez. Puede ver todos los usuarios, todos sus pedidos y todos los datos de cada pedido.

Reglas:
- El rol vive en una columna `role` de la tabla `profiles`, nunca solo en metadata editable por el propio cliente desde el frontend.
- Ninguna pantalla de registro público puede crear una cuenta `admin`. El default siempre es `client`.
- La vista "todos los pedidos" / "todos los usuarios" se protege en dos capas: **RLS en Supabase** (capa de datos) y **validación en el backend Java** (capa de API). El frontend oculta la UI de admin a quien no corresponde, pero esa ocultación es solo cosmética — la seguridad real nunca depende del frontend.

## 3. Modelo de datos (Supabase / Postgres)

### `profiles`
Extiende a `auth.users` (uno a uno por `id`).

| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid | FK → `auth.users.id` |
| `role` | text | `client` \| `admin`, default `client` |
| `first_name` | text | Nombre |
| `last_name` | text | Apellido |
| `phone` | text | Teléfono de contacto |
| `address_text` | text | Dirección en texto |
| `address_lat` / `address_lng` | numeric, nullable | Se llenan si el usuario usa "Usar mi ubicación actual" (Geolocation API del navegador) |
| `created_at` | timestamptz | |

Se crea automáticamente con un trigger al registrarse en Supabase Auth (`on auth.users insert`), para no depender de que el cliente llame a un endpoint aparte.

### `products`
Migra `PRODUCTS` y `CATEGORIES` del `index.html` a datos reales.

| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid/text | |
| `category` | text | `carne` \| `almacen` |
| `name` | text | |
| `detail` | text | Ej. "Entero o en porciones" |
| `unit` | text | `kg` \| `unidad` — reemplaza al `byKg` booleano del front |
| `price_per_unit` | numeric, nullable | `null` = placeholder "$ X.XXX" como ya hace el front |
| `tag` | text, nullable | "Más vendido", "Oferta", etc. |
| `active` | bool | default `true` |
| `created_at` | timestamptz | |

### `orders`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid | |
| `user_id` | uuid | FK → `profiles.id` |
| `status` | text | `pendiente_pago` \| `pago_en_revision` \| `confirmado` \| `preparando` \| `en_camino` \| `entregado` \| `cancelado` |
| `payment_method` | text | `mercadopago` \| `transferencia` \| `efectivo` |
| `payment_status` | text | `pendiente` \| `completado` \| `rechazado` |
| `delivery_type` | text | `envio` \| `retiro` |
| `delivery_address_text` / `_lat` / `_lng` | | Solo si `delivery_type = envio`; puede copiarse del perfil o cargarse distinto para ese pedido puntual |
| `subtotal_estimated` | numeric | Suma de `order_items.subtotal_estimated` |
| `total_estimated` | numeric | Igual que hoy en el front: "Total estimado", se ajusta al pesar la carne |
| `total_final` | numeric, nullable | Se completa cuando se pesa/confirma el pedido real |
| `notes` | text, nullable | Comentario general del pedido |
| `created_at` / `updated_at` | timestamptz | |

### `order_items`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid | |
| `order_id` | uuid | FK → `orders.id` |
| `product_id` | uuid/text | FK → `products.id` |
| `product_name_snapshot` | text | Copia del nombre al momento del pedido (si el producto cambia después, el pedido no se altera) |
| `unit` | text | `kg` \| `unidad` |
| `quantity` | numeric | Kg reales (ej. `1.5`) o unidades (ej. `3`) |
| `price_per_unit_snapshot` | numeric, nullable | Precio vigente al pedir |
| `subtotal_estimated` | numeric | `quantity * price_per_unit_snapshot` |
| `item_notes` | text, nullable | "sin grasa", "en bifes de 2 cm", etc. — ya contemplado como idea en `AGENTS.md` |

### `payments`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid | |
| `order_id` | uuid | FK → `orders.id` |
| `method` | text | `mercadopago` \| `transferencia` \| `efectivo` |
| `status` | text | `pendiente` \| `completado` \| `rechazado` |
| `mercadopago_payment_id` / `mercadopago_preference_id` | text, nullable | Solo si `method = mercadopago` |
| `transfer_marked_by_client_at` | timestamptz, nullable | Cuando el cliente aprieta "Ya transferí" |
| `amount` | numeric | |
| `created_at` / `confirmed_at` | timestamptz | `confirmed_at` lo completa el admin (transferencia/efectivo) o el webhook (MercadoPago) |

### `admin_notifications`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid | |
| `order_id` | uuid | FK → `orders.id` |
| `type` | text | `nuevo_pedido` \| `transferencia_marcada` \| `mercadopago_confirmado` \| `efectivo_pendiente` |
| `read` | bool | default `false` |
| `created_at` | timestamptz | |

Esta tabla alimenta la bandeja de notificaciones del panel admin. Se recomienda **Supabase Realtime** suscripto a `admin_notifications` para que el panel admin se actualice solo, sin recargar la página, cuando entra un pedido nuevo.

## 4. Flujo de compra

1. El catálogo se navega sin login (igual que hoy).
2. Para pagar, hace falta estar logueado. Si no tiene cuenta, se registra con Supabase Auth.
3. Checkout en pasos (ya sugerido en `AGENTS.md`):
   - Datos de contacto, prellenados desde `profiles` (nombre, apellido, teléfono).
   - Dirección: la del perfil, otra para ese pedido, o botón **"Usar mi ubicación actual"** (Geolocation API + reverse geocoding) si elige envío. Si elige retiro, no hace falta dirección.
   - Método de pago: MercadoPago / transferencia / efectivo.
4. Según el método elegido:
   - **MercadoPago:** el backend Java crea una *Preference* con el SDK oficial, el cliente paga en el checkout de MercadoPago. El **webhook** de MercadoPago le pega al backend, que marca `payment_status = completado`, `status = confirmado` y genera un `admin_notifications` de tipo `mercadopago_confirmado`.
   - **Transferencia:** se muestran los datos de la cuenta (placeholders: `[CBU]`, `[ALIAS]`, `[TITULAR]` — nunca inventar estos datos, van entre corchetes hasta tenerlos reales, siguiendo la regla de `AGENTS.md`). El cliente tiene un botón **"Ya transferí"**, que marca `payment_status = pendiente` → pasa a un estado de aviso (no se marca como pagado automáticamente, porque nadie lo verificó todavía) y dispara `admin_notifications` tipo `transferencia_marcada`. El admin entra al pedido, confirma manualmente que vio la transferencia en el banco, y recién ahí cambia `payment_status = completado`.
   - **Efectivo:** el pedido queda `status = confirmado` directo, `payment_status = pendiente`. Se dispara `admin_notifications` tipo `efectivo_pendiente`. El admin cobra al entregar y marca `payment_status = completado` desde el panel.
5. En los tres casos el admin se entera por el panel (no por mail) con el pedido completo: productos (cantidad en kg o unidades, precio por kg/unidad, subtotal), datos del cliente (nombre, apellido, dirección, teléfono) y método de pago elegido.

## 5. Paneles de usuario

- **Panel cliente (`/mis-pedidos`):** lista solo los pedidos de `user_id = auth.uid()`, con su estado y detalle. Nunca ve pedidos ni datos de otros usuarios.
- **Panel admin (`/admin`):** lista **todos** los pedidos de todos los usuarios, con filtros por estado / método de pago / fecha, bandeja de notificaciones, y al entrar a un pedido ve el detalle completo (productos, cantidades, precios, subtotales, datos del cliente completos). También puede entrar a ver cualquier usuario y su historial completo de pedidos.
- Las rutas de admin se protegen en el frontend (redirigir si `role != admin`) solo como UX; la seguridad real es RLS + validación de rol en cada endpoint del backend Java.

## 6. Supabase Auth — cuidado con el envío de mails

Supabase Auth manda mail de confirmación en cada registro nuevo. El plan gratuito tiene cuota baja de mails transaccionales, así que **durante el desarrollo hay que probar el registro lo menos posible**:

- Reutilizar uno o dos usuarios de prueba fijos en vez de registrar uno nuevo por cada prueba.
- Si hace falta variar el email para probar el alta, usar alias con `+` (`tuemail+test1@gmail.com`, `tuemail+test2@gmail.com`) en vez de direcciones nuevas.
- Evaluar desactivar "Confirm email" en **Authentication → Providers → Email** del proyecto de Supabase solo mientras se desarrolla, y volver a activarlo antes de ir a producción.
- Nunca automatizar registros en loop (scripts de carga, tests que registran en cada corrida, etc.).

## 7. Backend Java — arquitectura en capas (obligatoria)

- **Spring Boot 4 sobre Java 21**, build con Maven (la serie 3.x ya no está disponible en Maven Central al momento de implementar esto — llegó a su fin de soporte).
- Nada de controllers hablando directo con entidades JPA ni de lógica de negocio metida en el controller. Separación estricta por carpeta/paquete, cada una con una responsabilidad única:

```
src/main/java/.../carniceria/
├── controller/        # REST. Recibe/devuelve DTO. Sin lógica de negocio, solo orquesta service + mapea HTTP.
├── dto/
│   ├── request/        # Lo que entra por la API (ej. RegisterRequestDto, CreateOrderRequestDto)
│   └── response/        # Lo que sale por la API (ej. OrderResponseDto, ProfileResponseDto)
├── model/              # Reglas de negocio puras. Objetos de dominio SIN anotaciones de JPA ni de HTTP.
│                       # Ej.: Order, OrderItem, Payment como clases de dominio que validan sus propias
│                       # invariantes (cantidad > 0, no se puede confirmar un pago ya rechazado, etc.).
│                       # No conocen Spring, no conocen la base.
├── entity/             # Persistencia. Clases @Entity de JPA (OrderEntity, ProfileEntity, etc.),
│                       # mapeadas 1:1 a las tablas de la sección 3. Solo anotaciones JPA, sin reglas de negocio.
├── repository/         # Spring Data JPA. Interfaces sobre las entity, sin lógica.
├── service/            # Orquesta el caso de uso: repository → entity → model (aplica reglas de negocio) →
│                       # entity → repository, y devuelve model o dto al controller. Acá vive la lógica
│                       # de los flujos de pago, el cambio de estados de un pedido, etc.
├── mapper/             # Traducción explícita entre las 3 representaciones: dto ↔ model ↔ entity.
│                       # (a mano o con MapStruct, pero nunca mezclado dentro del service o el controller).
├── security/           # Filtro de validación del JWT de Supabase + resolución de rol.
├── config/             # Beans de configuración (CORS, MercadoPago client, etc.).
└── exception/          # Excepciones de dominio + manejador global (@ControllerAdvice).
```

- **Flujo de una request:** `controller` recibe un `dto.request` → `mapper` lo convierte a `model` → `service` aplica las reglas de negocio sobre el `model` → `mapper`/`service` lo convierte a `entity` para persistir vía `repository` → la respuesta se mapea de vuelta a `dto.response`. El controller nunca ve una `entity` ni un `model` crudo en su firma pública; siempre DTO.
- `model` es la única capa donde viven las reglas de negocio (ej.: "un pedido en estado `entregado` no puede cambiar de método de pago", "la cantidad de un item por kg no puede ser negativa ni tener más de dos decimales"). Esas reglas se testean solas, sin Spring ni base de datos (ver sección 9).
- `entity` es un reflejo tonto de la tabla: getters/setters/anotaciones JPA, nada de validación de negocio ahí.
- Cada request autenticado llega con el JWT que emite Supabase Auth. El backend lo valida contra el JWKS de Supabase y **nunca confía en un claim de rol que venga del cliente sin cruzarlo con `profiles.role` en la base**.
- Integración con MercadoPago vía su SDK oficial de Java, encapsulada en su propio `service` (ej. `MercadoPagoService`), nunca llamada directo desde el controller. Credenciales (access token) solo en variables de entorno, nunca hardcodeadas ni commiteadas.
- Endpoint público de webhook `/api/webhooks/mercadopago`, validando que la notificación sea legítima antes de actualizar nada.

## 8. Frontend — convenciones

- Migración gradual de `frontend/index.html` a Tailwind CSS + TypeScript. El contenido, tono y componentes aprobados en `AGENTS.md` se portan, no se reinventan.
- Los tokens de `:root` se traducen 1:1 a `tailwind.config` (`theme.extend.colors.accent`, `.ink`, `.muted`, `.surface`, etc.) para no perder el sistema visual.
- `PRODUCTS`, `CATEGORIES` y `FAQS` dejan de estar hardcodeados en el HTML y pasan a consumirse desde la API del backend (que a su vez lee de Supabase).
- El carrito puede seguir viviendo en `localStorage` hasta el checkout; a partir de ahí, se persiste como `orders` + `order_items` reales en el backend.
- Framework de frontend (vanilla TS con Vite vs. algo como React) queda abierto hasta que se arranque la migración — no se decide de más por ahora.

## 9. Testing — obligatorio, no opcional

Ningún flujo de negocio (pagos, cambio de estado de pedido, roles) se considera terminado sin sus tests. Dos niveles, los dos obligatorios:

### 9.1 Tests unitarios
- **Qué cubren:** la capa `model` (reglas de negocio puras, sin Spring ni base: ej. `OrderTest` probando que no se puede confirmar un pago rechazado, que la cantidad por kg no admite negativos) y la capa `service` (orquestación, con `repository` y clientes externos —MercadoPago— **mockeados**, nunca pegándole a una base real).
- **Herramientas:** JUnit 5 + Mockito + AssertJ.
- Viven junto a la clase que testean, en `src/test/java/.../<mismo paquete>`, con sufijo `Test` (ej. `OrderServiceTest`, `OrderTest`, `PaymentMapperTest`).
- Un mapper (`dto ↔ model ↔ entity`) también se testea unitariamente: dado un objeto de un lado, el otro sale con los campos correctos.

### 9.2 Tests end-to-end / integración
- **Qué cubren:** el flujo completo HTTP → `controller` → `service` → `repository` → base real, para los casos de uso críticos: registro/login, crear pedido completo con sus items, los tres flujos de pago (marcar transferencia, webhook de MercadoPago simulado, confirmar efectivo), y que un `client` nunca pueda leer pedidos de otro `client` ni acceder a rutas de `admin`.
- **Herramientas:** `@SpringBootTest` + `MockMvc` (o `RestAssured`) contra una base de test real levantada con **Testcontainers** (Postgres), nunca contra el Supabase de desarrollo/producción ni con mocks de la base.
- Viven en `src/test/java/.../e2e/`, sufijo `IT` (ej. `OrderFlowIT`, `AuthFlowIT`), separados de los unitarios para poder correrlos como un step distinto en CI (los unitarios corren siempre y rápido; los `IT` son más lentos).
- Casos mínimos que todo flujo de pago necesita cubrir en su `IT`: camino feliz, intento de un `client` de ver/modificar un pedido ajeno (debe fallar), y transición de estado inválida (ej. marcar `completado` un pago ya `rechazado`, debe rechazarse).

### 9.3 Frontend
- Tests unitarios de lógica de carrito/checkout (cálculo de subtotal estimado, validaciones de formulario) con **Vitest**.
- Al menos un test end-to-end del camino feliz de compra (agregar productos → checkout → confirmar pedido) con **Playwright**, por cada método de pago.

### 9.4 Regla general
- No se mergea un flujo de negocio nuevo sin al menos: sus tests unitarios de `model`/`service` + un `IT` del camino feliz. Edge cases adicionales se agregan a medida que se identifican, no hace falta cubrir el 100% desde el día uno, pero **pagos y permisos (admin vs. client) nunca van sin test.**

## 10. Seguridad — no negociables

- **RLS activado desde el día uno** en `profiles`, `orders`, `order_items`, `payments`, `admin_notifications`. Política general: cada usuario accede solo a sus propias filas (`user_id = auth.uid()`); el rol `admin` tiene policy aparte que le da acceso a todo.
- La **service role key** de Supabase vive únicamente en el backend Java (variables de entorno del servidor). Nunca se expone en el frontend.
- No se guardan datos de tarjetas. MercadoPago maneja esa parte; solo se persisten IDs de pago/preferencia.
- Todas las credenciales (Supabase, MercadoPago) van por variables de entorno. Nada de eso se commitea al repo.

## 11. Datos todavía sin definir (placeholders)

Igual que en `AGENTS.md`: nunca inventar estos valores, dejarlos entre corchetes hasta tenerlos reales.

- `[CBU]` / `[ALIAS]` / `[TITULAR]` de la cuenta para transferencias.
- Credenciales de MercadoPago (`[MP_ACCESS_TOKEN]` de TEST primero, luego PROD).
- Dominio/hosting definitivo del frontend y del backend.
- URL del proyecto Supabase y sus keys (`[SUPABASE_URL]`, `[SUPABASE_ANON_KEY]`, `[SUPABASE_SERVICE_ROLE_KEY]`).

## 12. Flujo de trabajo: Spec-Driven Development

Este documento fija los principios y decisiones que no cambian feature a feature (stack, modelo de datos, roles, arquitectura en capas, testing). Cada funcionalidad concreta se especifica aparte, en `specs/<NNN>-<nombre>/`, con tres archivos:

- `spec.md` — **qué** hay que construir y **por qué** (historias de usuario, criterios de aceptación, fuera de alcance). Sin detalles de implementación salvo los que ya fija esta constitution.
- `plan.md` — **cómo** se construye, aplicando la arquitectura en capas (sección 7) y el stack (sección 1) a esa feature puntual: qué clases en `entity`/`model`/`dto`/`service`/`controller`/`mapper`, qué endpoints, qué se testea.
- `tasks.md` — lista de tareas chicas y ordenadas (tests antes que su implementación) derivadas del plan, listas para ejecutarse una por una.

La primera feature ya está especificada en [`specs/001-autenticacion-y-perfil/`](../specs/001-autenticacion-y-perfil/spec.md) (registro, login, perfil de cliente con Supabase Auth). Las siguientes features (catálogo, carrito/checkout, pagos, panel admin) se van agregando como `002-...`, `003-...`, etc., siguiendo el mismo patrón y el roadmap de la sección 13.

## 13. Roadmap sugerido (orden de implementación)

1. Crear las tablas en Supabase (sección 3) + políticas RLS.
2. Configurar Supabase Auth (login/registro) + trigger que crea la fila en `profiles` al registrarse.
3. Crear a mano la única cuenta `admin`.
4. Esqueleto del backend Java (Spring Boot) con la estructura en capas de la sección 7 (`controller`/`dto`/`model`/`entity`/`repository`/`service`/`mapper`) y validación de JWT de Supabase, con su primer test unitario y su primer `IT` desde el día uno.
5. Migrar `PRODUCTS` / `CATEGORIES` / `FAQS` del `index.html` a la tabla `products` (seed).
6. Checkout: datos de contacto → envío/retiro (con opción de ubicación actual) → método de pago.
7. Implementar primero los flujos de **efectivo** y **transferencia** (no dependen de una API externa).
8. Integrar MercadoPago (preference + webhook).
9. Panel cliente "Mis pedidos".
10. Panel admin: todos los pedidos + bandeja de notificaciones + detalle completo por pedido.
11. Migrar el frontend estático a Tailwind + TypeScript conectado a la API real.
