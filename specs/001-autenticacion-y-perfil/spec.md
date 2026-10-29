# Spec 001 — Autenticación y perfil de cliente

Estado: borrador para revisión. Primera feature del flujo SDD, base de todo lo demás (sin cuenta no hay pedido, sin perfil completo el admin no puede despachar nada).

## Por qué

Antes de catálogo, carrito o pagos, el sistema necesita que una persona pueda registrarse, loguearse, y dejar los datos con los que el admin va a poder contactarla y entregarle el pedido (nombre, apellido, dirección, teléfono). También necesita que exista, desde el día uno, la distinción entre `client` y la única cuenta `admin`.

## Alcance de esta spec

Incluye:
- Registro e inicio de sesión de clientes.
- Completar y editar el perfil propio (nombre, apellido, dirección, teléfono).
- Uso opcional de la ubicación actual para completar la dirección.
- Que el login reconozca el rol (`client` vs. la única cuenta `admin`) y lo use para decidir qué panel corresponde.
- Que el admin pueda ver el perfil de cualquier cliente (prerequisito para los pedidos, que se especifican en una feature futura).

No incluye (queda para specs futuras): catálogo, carrito, checkout, pagos, panel de pedidos del admin, verificación de teléfono por SMS, login social.

## Actores

- **Cliente** (`role = client`): cualquiera que se registra desde la web.
- **Administrador** (`role = admin`): una sola cuenta, provisionada a mano antes de que exista esta feature en producción (no hay pantalla que la cree).

## Historias de usuario y criterios de aceptación

**US1 — Registro.** Como visitante, quiero crear una cuenta con email y contraseña para poder comprar más adelante.
- Dado un email que no existe y una contraseña válida, al confirmar se crea la cuenta con `role = client` y una fila de perfil asociada (puede estar incompleta).
- Si el email ya está registrado, veo un mensaje de error claro y no se crea una cuenta duplicada.
- Ninguna combinación de datos ingresados desde este formulario puede resultar en una cuenta `role = admin`.

**US2 — Login.** Como cliente registrado, quiero iniciar sesión con mi email y contraseña.
- Con credenciales correctas, accedo a mi sesión.
- Con credenciales incorrectas, veo un error genérico que no revela si el email existe o no.

**US3 — Completar/editar perfil.** Como cliente, quiero guardar mi nombre, apellido, dirección y teléfono para que mis pedidos futuros lleguen bien.
- Puedo editar estos datos cuantas veces quiera después de registrarme.
- Nombre, apellido y teléfono son obligatorios para considerar el perfil "completo" (no para poder registrarme).
- La dirección puede guardarse vacía en esta feature; se vuelve obligatoria recién en el checkout (feature futura), si el cliente elige envío.

**US4 — Usar ubicación actual.** Como cliente, quiero poder autocompletar mi dirección con mi ubicación actual en vez de escribirla.
- Si acepto el permiso de ubicación del navegador, se completan latitud/longitud y, si es posible, una dirección en texto.
- Si rechazo el permiso o falla la detección, puedo seguir escribiendo la dirección a mano sin que nada se bloquee.

**US5 — Rol en el login.** Como admin, al loguearme el sistema me reconoce como tal (sin que yo tenga que elegir nada) y queda disponible para decidir a qué panel ir.
- No existe ningún flujo público que permita auto-asignarse `role = admin`.
- El rol siempre se resuelve contra la fila de `profiles` en el servidor, nunca contra algo que mande el cliente.

**US6 — Recuperar contraseña (no bloqueante).** Como cliente, quiero poder recuperar mi contraseña si la olvido. Deseable en esta feature, pero no bloquea el cierre de la spec si se posterga.

## Restricciones heredadas de `constitution.md`

- Auth vía **Supabase Auth** (sección 1 y 6).
- Una sola cuenta `admin`, provisión manual, nunca self-signup (sección 2).
- Tabla `profiles` tal como está definida en la sección 3.
- RLS: cada cliente lee/edita solo su propia fila; `admin` accede a todas (sección 2 y 10).
- Probar el registro lo menos posible en desarrollo para no gastar la cuota de mails de Supabase (sección 6).
- Arquitectura en capas y tests obligatorios como están definidos en las secciones 7 y 9 — se detallan para esta feature en `plan.md` y `tasks.md` de esta misma carpeta.

## Cómo se valida esta feature (criterios de cierre)

- Un usuario nuevo puede registrarse, loguearse y guardar su perfil sin errores.
- Un cliente que intenta leer o editar el perfil de otro cliente recibe un rechazo (no hace falta que sea 403 exacto, pero nunca puede ver los datos).
- El admin puede ver el perfil de cualquier cliente.
- Todo lo anterior tiene al menos un test automatizado que lo demuestre (ver `plan.md`).

## Decisiones tomadas durante la implementación

- Reverse geocoding para US4: **Nominatim (OpenStreetMap)**, gratuito y sin API key (ver `plan.md`).
- US6 (recuperar contraseña) pasa a backlog — no bloqueaba el cierre de esta spec.
