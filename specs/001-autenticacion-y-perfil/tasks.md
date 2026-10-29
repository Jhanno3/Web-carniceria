# Tasks 001 — Autenticación y perfil de cliente

Deriva de `plan.md`. Orden sugerido; `[P]` = se puede hacer en paralelo con la tarea anterior porque no depende de ella. Los tests van antes de la implementación que los hace pasar (TDD), como pide la sección 9 de la constitution.

## Supabase

- [x] T1. Tabla `profiles` + trigger `on insert` en `auth.users` — aplicado en Supabase (vía Session pooler, la conexión directa es IPv6-only y no resuelve desde esta red).
- [x] T2. Políticas RLS en `profiles` — las 4 políticas verificadas en la base real.
- [x] T3. Cuenta `admin` creada vía Admin API de Supabase (email confirmado, sin gastar cuota de mail) y promovida a `role = 'admin'` en `profiles`. Credenciales las tiene el usuario; no quedaron guardadas en ningún archivo del repo.

## Backend — capa `model`

- [x] T4. [P] Test unitario `ProfileTest`: teléfono inválido se rechaza; nombre/apellido vacíos impiden marcar el perfil como "completo"; no existe ningún setter de `role` en el modelo.
- [x] T5. Implementar `model.Profile` hasta que T4 pase.

## Backend — capa `entity` + `mapper`

- [x] T6. [P] Test unitario `ProfileMapperTest`: `entity → model`, `model → entity`, `model → dto.response`, `dto.request + model → model` (update parcial).
- [x] T7. Implementar `entity.ProfileEntity` y `mapper.ProfileMapper` hasta que T6 pase.

## Backend — capa `repository` + `service`

- [x] T8. [P] Test unitario `ProfileServiceTest` (repository mockeado): `getMyProfile` camino feliz; `updateMyProfile` ignora `role` si viniera en el dto; `getProfileById` rechaza a un `client` pidiendo el perfil de otro `client`; `listAllProfiles` rechaza a quien no es `admin`.
- [x] T9. Implementar `repository.ProfileRepository` y `service.ProfileService` hasta que T8 pase.

## Backend — seguridad

- [x] T10. Implementar `security.SupabaseJwtAuthFilter`: valida el JWT contra el JWKS de Supabase, resuelve `userId`, busca el `role` real en `ProfileRepository` (nunca confía en el claim del token) y lo deja en el contexto de Spring Security.

## Backend — capa `dto` + `controller`

- [x] T11. Implementar `dto.request.UpdateProfileRequestDto` y `dto.response.ProfileResponseDto`.
- [x] T12. Implementar `controller.ProfileController` con los 4 endpoints de `plan.md`, delegando todo a `ProfileService`.

## Backend — end-to-end

- [x] T13. Test `AuthProfileFlowIT` (Testcontainers + JWT de test firmado localmente): camino feliz de `GET`/`PUT /api/profile/me`; `client` A no puede leer el perfil de `client` B; `admin` lista todos los perfiles; `PUT /api/profile/me` con `role` en el body no cambia el rol. **Verificado**: `./mvnw verify` → 4/4 pasan contra Postgres real.
- [x] T14. `maven-failsafe-plugin` agregado al `pom.xml`, bindeado a `integration-test`/`verify`. Confirmado en la práctica: `mvn test` ya no toca nada de Testcontainers/Docker (antes el smoke test default de Initializr sí lo hacía — se renombró a `BackendApplicationIT` en `e2e/` por la misma razón).

## Frontend

- [x] T15. `login.html`/`registro.html` + `src/pages/login.ts`/`registro.ts`, usando `@supabase/supabase-js` directo (sin pasar por el backend). Proyecto Vite + TypeScript + Tailwind v4 armado en `frontend/` (`npm install`, `npm run dev`), portando los tokens de `AGENTS.md` a `src/styles/app.css`. `index.html` (la landing) queda intacto.
- [x] T16. `perfil.html` + `src/pages/perfil.ts`: carga el perfil con `GET /api/profile/me`, guarda con `PUT`, guard que redirige a `/login.html` si no hay sesión.
- [x] T17. Botón "Usar mi ubicación actual" con `navigator.geolocation` + reverse geocoding vía **Nominatim (OpenStreetMap)** — decisión tomada: gratuito, sin API key. Fallback a edición manual si se rechaza el permiso o falla el geocoding.
- [x] T18. [P] `src/lib/validation.test.ts` (Vitest): 8/8 tests en verde — teléfono inválido, nombre/apellido vacíos, email inválido, contraseña corta.
- [x] T19. `tests/e2e/auth-profile.spec.ts` (Playwright) escrito: registro → login → completar perfil → logout → login de nuevo. **Gateado** detrás de `RUN_SIGNUP_E2E=1` (no corre con `npm run test:e2e` normal) para no gastar la cuota de mail de Supabase, como pide la sección 6 de la constitution — requiere además "Confirm email" en OFF en el proyecto de Supabase usado para testear. No se corrió todavía (decisión del usuario si vale la pena gastar un registro real para verificarlo).

## Cierre

- [x] T20. Cobertura por criterio de aceptación de `spec.md`:
  - US1 (registro): `validateRegisterForm` (Vitest) + `registro.ts` + Playwright gateado. Probado manualmente end-to-end con la cuenta admin real (Supabase Auth Admin API → backend → `profiles`).
  - US2 (login): `login.ts` + Playwright gateado. Probado manualmente: login real contra Supabase devolvió JWT ES256, validado por el backend.
  - US3 (completar/editar perfil): `ProfileTest`, `ProfileServiceTest`, `AuthProfileFlowIT` (backend) + `validateProfileForm` (Vitest) + `perfil.ts`. Probado manualmente con `GET`/`PUT /api/profile/me` reales.
  - US4 (ubicación actual): implementado en `perfil.ts`, **sin test automatizado** (requiere mockear `navigator.geolocation` en Playwright — queda como mejora pendiente, no bloqueante).
  - US5 (rol en el login): `AuthProfileFlowIT` (`admin` lista todos, `client` rechazado) + confirmado manualmente (`role: admin` resuelto correctamente desde el JWT real).
  - US6 (recuperar contraseña): **pasa a backlog**, no entra en esta iteración — no estaba marcada como bloqueante en `spec.md`.
