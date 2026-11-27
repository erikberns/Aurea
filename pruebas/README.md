# Pruebas del sistema completo

Pruebas que se corren **contra el sistema levantado** con `docker compose up --build -d` (las pruebas unitarias,
de integración, de seguridad y PIT del backend están en `aurea-backend` y corren con Maven).
Todos los resultados quedan en `pruebas/resultados/` (no se versiona).

| Qué | Umbral de la entrega | Herramienta | Cómo se corre | En el CI |
| --- | --- | --- | --- | --- |
| Carga: 50 usuarios, 5 min | p95 < 500 ms, errores < 1 % | k6 | ver abajo | perfil corto de humo |
| Colección de API: éxito y error por endpoint | 0 fallas | Newman | `npm run newman` | sí |
| 5 escenarios de punta a punta | 0 fallas | Playwright | `npm run e2e` | sí |
| Responsive 360 / 768 / 1280 px | sin desborde horizontal | Playwright | `npm run responsive` | sí |
| Accesibilidad | 90 o más | Lighthouse | `npm run lighthouse` | sí |
| Contrato REST contra OpenAPI | sin 5xx, respuestas según el contrato | Schemathesis | `sh contrato/schemathesis.sh` | sí |
| Contrato SOAP contra el WSDL | operaciones y campos que usa el adaptador | script Node | `npm run contrato-wsdl` | sí |
| Escaneo de seguridad dinámico | 0 alertas High | OWASP ZAP | `sh zap/zap.sh` | baseline |
| Dependencias e imágenes | 0 CVE CRITICAL | Trivy | `sh trivy.sh` | dependencias |

## Preparación (una vez)

```
cd pruebas/e2e
npm install
npx playwright install --with-deps chromium
```

`ADMIN_EMAIL` y `ADMIN_PASSWORD` son los mismos valores del `.env`; los necesitan Newman, el escenario
E2E-05 y Schemathesis. Nunca se escriben en estos archivos.

## Comandos

PowerShell (Windows), desde `pruebas/e2e`:

```
$env:ADMIN_PASSWORD = "<el valor del .env>"
npm run newman
npm run e2e
npm run responsive
npm run lighthouse
npm run contrato-wsdl
```

k6 (Docker), desde la raíz del repo:

```
docker run --rm -i -v "${PWD}/pruebas:/pruebas" -e BASE_URL=http://host.docker.internal:5173 grafana/k6 run /pruebas/k6/carga.js
```

ZAP, Schemathesis y Trivy son scripts `sh` (en Windows, desde Git Bash). En Windows/Mac los contenedores
llegan al sistema por `host.docker.internal`:

```
RED=bridge OBJETIVO=http://host.docker.internal:5173 sh pruebas/zap/zap.sh               # baseline
RED=bridge OBJETIVO=http://host.docker.internal:5173 MODO=completo sh pruebas/zap/zap.sh # activo
RED=bridge API=http://host.docker.internal:8080 MODO=api sh pruebas/zap/zap.sh           # API por OpenAPI
RED=bridge API_URL=http://host.docker.internal:8080 ADMIN_PASSWORD=... sh pruebas/contrato/schemathesis.sh
sh pruebas/trivy.sh
```

En Linux (y en el CI) se usan los valores por defecto (`RED=host`, `localhost`).

## Estabilidad de la suite

La entrega pide 0 tests inestables: correr la suite del backend 3 veces seguidas y comparar.

```
cd aurea-backend
1..3 | % { .\mvnw.cmd -B -q clean verify; "Corrida $_ : código $LASTEXITCODE" }
```
