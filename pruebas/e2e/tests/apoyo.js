// Utilidades compartidas por los escenarios: datos únicos por corrida y pasos repetidos.
import { expect } from "@playwright/test";

export const CLAVE = "ClaveSegura123";

export function emailUnico(prefijo = "e2e") {
  return `${prefijo}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`;
}

/** Registra una cuenta nueva desde el formulario y espera llegar a "Mi cuenta". */
export async function registrarse(page, email = emailUnico()) {
  await page.goto("/registro");
  await page.locator('input[name="nombre"]').fill("Clara");
  await page.locator('input[name="apellido"]').fill("Prueba");
  await page.locator('input[name="email"]').fill(email);
  await page.locator('input[name="contrasena"]').fill(CLAVE);
  await page.locator('input[name="confirmarContrasena"]').fill(CLAVE);
  await page.locator('form button[type="submit"]').click();
  await expect(page).toHaveURL(/\/cuenta$/);
  await expect(page.getByRole("heading", { name: /Hola, Clara/ })).toBeVisible();
  return email;
}

export async function iniciarSesion(page, email, clave) {
  await page.goto("/login");
  await page.locator('input[name="email"]').fill(email);
  await page.locator('input[name="contrasena"]').fill(clave);
  await page.locator('form button[type="submit"]').click();
}

/** Abre la primera joya del catálogo, la agrega a la bolsa y devuelve su nombre. */
export async function agregarPrimeraJoya(page) {
  await page.goto("/catalogo");
  const primera = page.locator('a[href^="/productos/"]').first();
  await expect(primera).toBeVisible();
  await primera.click();
  await expect(page).toHaveURL(/\/productos\/\d+$/);
  const nombre = (await page.locator("h1").first().textContent())?.trim();
  await page.getByRole("button", { name: /Añadir a la Bolsa/i }).click();
  await expect(page.getByRole("button", { name: /Añadido a la bolsa/i })).toBeVisible();
  return nombre;
}

/** En el checkout: dirección, "Confirmar pedido", elegir tarjeta de prueba y pagar. */
export async function pagar(page, { aprueba }) {
  await page.goto("/checkout");
  await page.getByPlaceholder(/Av\. Libertador/).fill("Av. Corrientes 1234, CABA");
  await page.getByRole("button", { name: "Confirmar pedido" }).click();
  const opcion = page.locator("fieldset label").filter({ hasText: aprueba ? "Aprueba" : "Rechaza" }).first();
  await opcion.locator('input[type="radio"]').check();
  await page.getByRole("button", { name: /^Pagar/ }).click();
}
