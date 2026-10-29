# Plan 001 — Autenticación y perfil de cliente

Traduce `spec.md` de esta misma carpeta a diseño técnico, siguiendo el stack y la arquitectura fijados en [constitution.md](../../agents/constitution.md). No repite reglas generales de la constitution, solo cómo se aplican a esta feature puntual.

## Decisión de responsabilidades

- **Registro, login, reset de contraseña:** los maneja el **SDK de Supabase Auth directo desde el frontend**. El backend Java no reimplementa estos endpoints — Supabase ya los expone y maneja el envío de mails.
- **Perfil (leer/editar/listar):** lo maneja el **backend Java**, protegido con el JWT que emite Supabase Auth. Es la única parte de esta feature con lógica de negocio propia.
- **Creación de la fila `profiles`:** un trigger SQL en Supabase (`on insert` en `auth.users`) inserta automáticamente en `profiles` con `role = 'client'` y los campos de nombre/apellido/teléfono/dirección en `null`. Así el backend Java nunca necesita un endpoint de "crear perfil" aparte del registro.

## Capas del backend involucradas (paquete `profile`)

| Capa | Clase | Responsabilidad |
|---|---|---|
| `entity` | `ProfileEntity` | Mapeo 1:1 a la tabla `profiles` (sección 3 de la constitution). Solo anotaciones JPA. |
| `model` | `Profile` | Reglas de negocio: nombre/apellido/teléfono no vacíos para considerarse "completo"; formato de teléfono válido; nadie puede cambiarse el propio `role`. |
| `dto.request` | `UpdateProfileRequestDto` | `firstName`, `lastName`, `phone`, `addressText`, `addressLat`, `addressLng`. Sin `role` — el rol nunca se acepta desde un DTO de entrada. |
| `dto.response` | `ProfileResponseDto` | Igual que la entidad pero sin nada sensible de más. |
| `repository` | `ProfileRepository` | `extends JpaRepository<ProfileEntity, UUID>`. |
| `service` | `ProfileService` | `getMyProfile(userId)`, `updateMyProfile(userId, dto)`, `getProfileById(requesterRole, requesterId, targetId)` (solo pasa si `requesterRole == admin` o `requesterId == targetId`), `listAllProfiles(requesterRole)` (solo admin). |
| `controller` | `ProfileController` | Ver tabla de endpoints abajo. Sin lógica propia, delega todo a `ProfileService`. |
| `mapper` | `ProfileMapper` | `toModel(ProfileEntity)`, `toEntity(Profile)`, `toResponseDto(Profile)`, `applyUpdate(Profile, UpdateProfileRequestDto)`. |
| `security` | `SupabaseJwtAuthFilter` | Valida el JWT contra el JWKS de Supabase, extrae el `userId`, busca el `role` real en `ProfileRepository` (nunca confía en un claim de rol del token) y lo deja disponible como `Authentication` de Spring Security. |

## Endpoints

| Método | Path | Quién puede | Qué hace |
|---|---|---|---|
| `GET` | `/api/profile/me` | Cualquier usuario autenticado | Devuelve el perfil propio. |
| `PUT` | `/api/profile/me` | Cualquier usuario autenticado | Edita nombre/apellido/teléfono/dirección propios. Ignora cualquier intento de mandar `role`. |
| `GET` | `/api/profiles/{id}` | `admin`, o el propio usuario si `id` es el suyo | Devuelve el perfil de otro usuario. Para cualquier otro caso: rechazado. |
| `GET` | `/api/profiles` | Solo `admin` | Lista todos los perfiles. |

## Geolocalización (frontend)

1. Botón "Usar mi ubicación actual" dispara `navigator.geolocation.getCurrentPosition`.
2. Si el usuario la permite: se obtiene lat/lng, se intenta reverse geocoding con **Nominatim (OpenStreetMap)** — gratuito, sin API key — para completar el texto de la dirección.
3. Si falla el permiso o el geocoding: los campos de dirección quedan editables a mano, sin bloquear el guardado del resto del perfil.

## Tests de esta feature (según sección 9 de la constitution)

### Unitarios
- `ProfileTest` (capa `model`): rechaza teléfono con formato inválido; rechaza "completar perfil" con nombre/apellido vacíos; no permite mutar `role` desde el propio modelo.
- `ProfileMapperTest`: `entity → model → dto` y vuelta, con los campos esperados en cada sentido.
- `ProfileServiceTest` (con `ProfileRepository` mockeado): camino feliz de `getMyProfile`/`updateMyProfile`; un `client` que pide el perfil de otro `client` es rechazado; un `admin` que pide cualquier perfil es aceptado.

### End-to-end
- `AuthProfileFlowIT` (`@SpringBootTest` + `MockMvc`/`RestAssured` + Testcontainers Postgres, nunca contra el Supabase real):
  1. Camino feliz: `GET` y `PUT` de `/api/profile/me` con un JWT de test válido.
  2. Un `client` A intentando `GET /api/profiles/{idDeB}` → rechazado.
  3. Un `admin` haciendo `GET /api/profiles` → devuelve todos los perfiles de prueba cargados.
  4. Intento de mandar `role` en el body de `PUT /api/profile/me` → se ignora, el rol no cambia.
- Cómo se evita gastar la cuota real de mails de Supabase en estos tests: el JWT de test se firma localmente con una key de prueba (no se registra un usuario real en el Supabase de desarrollo en cada corrida de CI). El flujo de registro contra Supabase real se prueba manualmente y pocas veces, como ya indica la sección 6 de la constitution.

### Frontend
- Vitest: validación del formulario de perfil (teléfono inválido, nombre vacío) antes de pegarle a la API.
- Playwright: un camino feliz registro → login → completar perfil. Se corre pocas veces en local/CI contra un proyecto de Supabase de test, nunca contra producción.

## Cerrado

- Proveedor de reverse geocoding: **Nominatim (OpenStreetMap)**. Si el tráfico crece y se pega contra el límite de uso gratuito, evaluar Google Maps Geocoding (de pago) o un self-host de Nominatim.
- US6 (recuperar contraseña): queda en backlog, no entró en esta iteración (no era bloqueante en `spec.md`).
