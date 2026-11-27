// Cinco escenarios de punta a punta sobre el sistema completo (eje A de la entrega final).
// Cada uno usa datos propios (cuentas y productos nuevos) para poder correr en cualquier orden y repetirse.
import { test, expect } from "@playwright/test";
import { agregarPrimeraJoya, emailUnico, iniciarSesion, pagar, registrarse, CLAVE } from "./apoyo.js";

test("E2E-01 · Un visitante explora el catálogo, filtra y abre el detalle de una joya", async ({ page }) => {
  await page.goto("/catalogo");
  const tarjetas = page.locator('a[href^="/productos/"]');
  await expect(tarjetas.first()).toBeVisible();
  const cantidad = await tarjetas.count();

  // Filtro por rango de precios con mínimo mayor que el máximo: avisa del error, no muestra joyas
  // (no depende de qué productos haya en la base) y "Limpiar" restablece el catálogo.
  await page.getByLabel("Precio mínimo").fill("200000");
  await page.getByLabel("Precio máximo").fill("100000");
  await expect(page.getByText("El precio mínimo supera al máximo.")).toBeVisible();
  await expect(page.getByRole("heading", { name: "No encontramos joyas" })).toBeVisible();
  await expect(tarjetas).toHaveCount(0);
  await page.getByRole("button", { name: "Limpiar todos los filtros" }).click();
  await expect(tarjetas).toHaveCount(cantidad);

  await tarjetas.first().click();
  await expect(page).toHaveURL(/\/productos\/\d+$/);
  await expect(page.getByRole("button", { name: /Añadir a la Bolsa/i })).toBeVisible();
});

test("E2E-02 · Una persona se registra, cierra sesión y vuelve a ingresar", async ({ page }) => {
  const email = await registrarse(page);
  await page.getByRole("button", { name: "Cerrar Sesión" }).click();
  await expect(page).not.toHaveURL(/\/cuenta$/);

  // Una ruta protegida sin sesión lleva al login.
  await page.goto("/cuenta");
  await expect(page).toHaveURL(/\/login$/);

  // Contraseña incorrecta: se queda en el login con el formulario disponible.
  await iniciarSesion(page, email, "ClaveIncorrecta1");
  await expect(page.getByRole("button", { name: "Iniciar Sesión" })).toBeEnabled();
  await expect(page).toHaveURL(/\/login$/);

  await iniciarSesion(page, email, CLAVE);
  await expect(page).toHaveURL(/\/cuenta$/);
});

test("E2E-03 · Un cliente compra con tarjeta aprobada y ve el pedido en su cuenta", async ({ page }) => {
  await registrarse(page);
  await agregarPrimeraJoya(page);

  const aviso = page.waitForEvent("dialog");
  await pagar(page, { aprueba: true });
  const dialogo = await aviso;
  const texto = dialogo.message();
  expect(texto).toMatch(/Pago aprobado\. Pedido #\d+ confirmado/);
  const pedido = texto.match(/#(\d+)/)[1];
  await dialogo.accept();

  await expect(page).toHaveURL(/\/cuenta$/);
  await page.getByRole("button", { name: /Mis Pedidos/ }).click();
  await expect(page.getByText(`Orden #${pedido}`)).toBeVisible();
});

test("E2E-04 · Una tarjeta rechazada no genera pedido y la bolsa se conserva", async ({ page }) => {
  await registrarse(page);
  const nombre = await agregarPrimeraJoya(page);

  let huboDialogo = false;
  page.on("dialog", (d) => { huboDialogo = true; d.dismiss(); });
  await pagar(page, { aprueba: false });

  await expect(page.getByRole("alert").first()).toBeVisible();
  expect(huboDialogo).toBe(false);
  await expect(page).toHaveURL(/\/checkout$/);
  if (nombre) await expect(page.getByText(nombre).first()).toBeVisible();

  await page.goto("/cuenta");
  await page.getByRole("button", { name: /Mis Pedidos/ }).click();
  await expect(page.getByText("Aún no tenés pedidos.")).toBeVisible();
});

test("E2E-05 · El administrador publica una joya nueva y aparece en la tienda; un cliente no entra al panel", async ({ page, browser }) => {
  const adminEmail = process.env.ADMIN_EMAIL || "admin@aurea.com";
  const adminClave = process.env.ADMIN_PASSWORD;
  test.skip(!adminClave, "Definir ADMIN_PASSWORD (el mismo valor del .env) para correr este escenario.");

  await iniciarSesion(page, adminEmail, adminClave);
  await expect(page).toHaveURL(/\/cuenta$/);
  await page.goto("/admin/productos/nueva");

  const nombre = `Anillo E2E ${Date.now()}`;
  await page.getByLabel(/Nombre de la pieza/).fill(nombre);
  const categoria = page.getByLabel(/Categoría de catálogo/);
  await categoria.selectOption({ index: 1 });
  await page.getByLabel(/Descripción artesanal/).fill("Plata 925 con terminación pulida, creada por la prueba E2E.");
  await page.getByLabel(/Precio al público/).fill("43000");
  await page.getByLabel(/Stock en depósito/).fill("4");
  await page.getByRole("button", { name: /Guardar y publicar/ }).click();
  await expect(page).toHaveURL(/\/admin\/productos$/);
  await expect(page.getByText(nombre).first()).toBeVisible();

  await page.goto(`/catalogo?search=${encodeURIComponent(nombre)}`);
  await expect(page.getByText(nombre).first()).toBeVisible();

  // Un cliente que intenta entrar al panel vuelve a su cuenta (y la API le respondería 403).
  const otro = await browser.newContext();
  const cliente = await otro.newPage();
  await registrarse(cliente, emailUnico("e2e-cliente"));
  await cliente.goto("/admin");
  await expect(cliente).toHaveURL(/\/cuenta$/);
  await otro.close();
});
