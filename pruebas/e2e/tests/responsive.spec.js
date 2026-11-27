// Responsive: la tienda no debe desbordar horizontalmente en celular (360 px), tablet (768 px) ni escritorio (1280 px).
// Se corre en tres proyectos de Playwright y guarda una captura de cada página como evidencia.
import { test, expect } from "@playwright/test";

const PAGINAS = [
  { nombre: "inicio", ruta: "/" },
  { nombre: "catalogo", ruta: "/catalogo" },
  { nombre: "detalle", ruta: "/productos/1" },
  { nombre: "login", ruta: "/login" },
  { nombre: "registro", ruta: "/registro" },
  { nombre: "checkout", ruta: "/checkout" },
];

for (const { nombre, ruta } of PAGINAS) {
  test(`${nombre} sin desborde horizontal`, async ({ page }, info) => {
    await page.goto(ruta);
    await page.waitForLoadState("networkidle");
    const { ancho, visible, culpables } = await page.evaluate(() => {
      const visible = window.innerWidth;
      // Elementos que se salen por la derecha: ayudan a encontrar qué componente desborda.
      const culpables = [...document.querySelectorAll("body *")]
        .filter((el) => el.getBoundingClientRect().right > visible + 1 && el.getClientRects().length > 0)
        .slice(0, 5)
        .map((el) => `${el.tagName.toLowerCase()}.${String(el.className).split(" ").slice(0, 4).join(".")} → ${Math.round(el.getBoundingClientRect().right)}px`);
      return { ancho: document.documentElement.scrollWidth, visible, culpables };
    });
    await page.screenshot({
      path: `../resultados/responsive/${info.project.name}-${nombre}.png`,
      fullPage: true,
    });
    expect(ancho, `el contenido mide ${ancho}px y la pantalla ${visible}px. Se salen: ${culpables.join(" | ")}`).toBeLessThanOrEqual(visible + 1);
  });
}
