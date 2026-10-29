import { test, expect } from "@playwright/test";

/**
 * Camino feliz: registro -> login -> completar perfil (T19 de specs/001-autenticacion-y-perfil).
 *
 * OJO: este test registra un usuario REAL contra el proyecto de Supabase configurado en .env.local.
 * Por la sección 6 de constitution.md, NO hay que correrlo seguido (gasta la cuota de mail de
 * Supabase). Está gateado detrás de RUN_SIGNUP_E2E=1 a propósito: no corre con un `npm run test:e2e`
 * normal ni en CI por accidente.
 *
 * Requiere, en el proyecto de Supabase usado para testear (nunca el de producción):
 *   Authentication -> Providers -> Email -> "Confirm email" en OFF,
 * si no, signUp no devuelve sesión y el login inmediato después falla.
 *
 * Correr a mano con: RUN_SIGNUP_E2E=1 npm run test:e2e
 */
test.skip(!process.env.RUN_SIGNUP_E2E, "Gateado: registra un usuario real. Ver comentario del archivo.");

test("registro -> login -> completar perfil", async ({ page }) => {
  const email = `carnevalefacundo+pwtest${Date.now()}@gmail.com`;
  const password = "contrasena-de-test-123";

  await page.goto("/registro.html");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Contraseña").fill(password);
  await page.getByRole("button", { name: "Crear cuenta" }).click();

  await page.waitForURL("**/perfil.html");

  await page.getByLabel("Nombre").fill("Test");
  await page.getByLabel("Apellido").fill("Playwright");
  await page.getByLabel("Teléfono").fill("+5491112345678");
  await page.getByRole("button", { name: "Guardar" }).click();

  await expect(page.getByText("Perfil actualizado.")).toBeVisible();

  // Logout y login de nuevo, para probar también el camino de "ya tengo cuenta".
  await page.getByRole("button", { name: "Cerrar sesión" }).click();
  await page.waitForURL("**/login.html");

  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Contraseña").fill(password);
  await page.getByRole("button", { name: "Entrar" }).click();

  await page.waitForURL("**/perfil.html");
  await expect(page.getByLabel("Nombre")).toHaveValue("Test");
});
