#!/usr/bin/env sh
# Prueba de contrato REST con Schemathesis: genera peticiones a partir del contrato OpenAPI
# (GET /v3/api-docs) y verifica que la API nunca responda 5xx y que las respuestas documentadas respeten su esquema.
#
# Requiere el sistema levantado (docker compose up) y Docker. Variables:
#   API_URL         base del backend vista desde el contenedor (Linux: http://localhost:8080 con RED=host;
#                   Windows/Mac: RED=bridge y API_URL=http://host.docker.internal:8080)
#   ADMIN_EMAIL     y ADMIN_PASSWORD: los del .env; con ellos se prueba también la parte autenticada.
set -eu
API_URL="${API_URL:-http://localhost:8080}"
RED="${RED:-host}"
# Git Bash (Windows) convierte rutas tipo /zap/wrk en C:/Program Files/Git/...: se desactiva y se usa la ruta de Windows.
export MSYS_NO_PATHCONV=1
CARPETA="$(cd "$(dirname "$0")/.." && (pwd -W 2>/dev/null || pwd))"
mkdir -p "$CARPETA/resultados"

TOKEN=""
# Un .env con finales de línea de Windows deja un \r al final de la clave: se quita.
ADMIN_PASSWORD="$(printf '%s' "${ADMIN_PASSWORD:-}" | tr -d '\r')"
ADMIN_EMAIL="$(printf '%s' "${ADMIN_EMAIL:-admin@aurea.com}" | tr -d '\r')"
if [ -n "$ADMIN_PASSWORD" ]; then
  # Login de administrador para obtener un JWT (las peticiones con Bearer no necesitan token CSRF).
  # Sin archivo de cookies: en Git Bash (con MSYS_NO_PATHCONV) curl no encuentra /tmp. El token CSRF es el mismo
  # valor que la cookie XSRF-TOKEN, así que se manda en la cabecera y en la cookie.
  BASE_LOCAL="${LOCAL_URL:-http://localhost:8080}"
  CSRF=$(curl -fsS "$BASE_LOCAL/api/csrf" | sed -E 's/.*"token":"([^"]+)".*/\1/')
  TOKEN=$(curl -sS -D - -o /dev/null -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $CSRF" \
    -H "Cookie: XSRF-TOKEN=$CSRF" \
    -d "{\"email\":\"${ADMIN_EMAIL:-admin@aurea.com}\",\"contrasena\":\"$ADMIN_PASSWORD\"}" \
    "$BASE_LOCAL/api/usuarios/autenticar" | tr -d '\r' | sed -n 's/^[Aa]uthorization: Bearer //p')
  if [ -z "$TOKEN" ]; then
    echo "No se pudo iniciar sesión como administrador (revisar ADMIN_EMAIL y ADMIN_PASSWORD): se corta para no probar a medias."
    exit 1
  fi
fi

set -- run "$API_URL/v3/api-docs" \
  --url "$API_URL" \
  --checks not_a_server_error,response_schema_conformance \
  --max-examples "${EJEMPLOS:-25}" \
  --exclude-path-regex 'sugerencia-ia|/salir|/rol$|/contrasena$' \
  --report junit --report-junit-path /resultados/schemathesis-junit.xml
[ -n "$TOKEN" ] && set -- "$@" -H "Authorization: Bearer $TOKEN"

# Se excluyen: el asistente de IA (consume cuota del modelo), y salir/rol/contraseña porque revocarían
# o degradarían al administrador en medio de la corrida.
docker run --rm --network "$RED" -v "$CARPETA/resultados:/resultados" schemathesis/schemathesis:stable "$@"
