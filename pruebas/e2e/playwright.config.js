// Configuración de Playwright para Áurea Joyas.
// BASE_URL apunta a la tienda servida por nginx (docker compose). ADMIN_EMAIL y ADMIN_PASSWORD
// son los del .env y solo se usan en el escenario del panel de administración.
import { defineConfig, devices } from "@playwright/test";

const BASE_URL = process.env.BASE_URL || "http://localhost:5173";

export default defineConfig({
  testDir: "./tests",
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  outputDir: "../resultados/playwright-artefactos",
  reporter: [
    ["list"],
    ["html", { outputFolder: "../resultados/playwright-reporte", open: "never" }],
    ["junit", { outputFile: "../resultados/playwright-junit.xml" }],
  ],
  use: {
    baseURL: BASE_URL,
    locale: "es-AR",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [
    { name: "e2e", testMatch: /flujos\.spec\.js/, use: { ...devices["Desktop Chrome"], viewport: { width: 1280, height: 800 } } },
    { name: "responsive-celular", testMatch: /responsive\.spec\.js/, use: { ...devices["Desktop Chrome"], viewport: { width: 360, height: 740 }, isMobile: true, hasTouch: true } },
    { name: "responsive-tablet", testMatch: /responsive\.spec\.js/, use: { ...devices["Desktop Chrome"], viewport: { width: 768, height: 1024 }, hasTouch: true } },
    { name: "responsive-escritorio", testMatch: /responsive\.spec\.js/, use: { ...devices["Desktop Chrome"], viewport: { width: 1280, height: 800 } } },
  ],
});
