#!/usr/bin/env sh
# OWASP ZAP contra el sistema levantado con docker compose (eje D: 0 alertas High).
#   MODO=baseline  escaneo pasivo de la tienda (rápido, se usa en el CI)
#   MODO=completo  escaneo activo de la tienda (ataca formularios y parámetros; ~15-30 min)
#   MODO=api       escaneo activo de la API a partir del contrato OpenAPI (/v3/api-docs)
# Variables: OBJETIVO (tienda, por defecto http://localhost:5173), API (backend, http://localhost:8080),
#            RED (host en Linux; en Windows/Mac usar RED=bridge con OBJETIVO=http://host.docker.internal:5173
#            y API=http://host.docker.internal:8080).
set -u
MODO="${MODO:-baseline}"
OBJETIVO="${OBJETIVO:-http://localhost:5173}"
API="${API:-http://localhost:8080}"
RED="${RED:-host}"
# Git Bash (Windows) convierte rutas tipo /zap/wrk en C:/Program Files/Git/...: se desactiva y se usa la ruta de Windows.
export MSYS_NO_PATHCONV=1
CARPETA="$(cd "$(dirname "$0")/.." && (pwd -W 2>/dev/null || pwd))"
mkdir -p "$CARPETA/resultados/zap"
chmod 777 "$CARPETA/resultados/zap" 2>/dev/null || true

case "$MODO" in
  baseline) set -- zap-baseline.py -t "$OBJETIVO" -m 3 ;;
  completo) set -- zap-full-scan.py -t "$OBJETIVO" -m 5 ;;
  api)      set -- zap-api-scan.py -t "$API/v3/api-docs" -f openapi ;;
  *) echo "MODO desconocido: $MODO"; exit 2 ;;
esac

# -I: no cortar por advertencias (Medium/Low); la condición de la entrega se evalúa abajo (solo High).
docker run --rm --network "$RED" -v "$CARPETA/resultados/zap:/zap/wrk:rw" ghcr.io/zaproxy/zaproxy:stable \
  "$@" -I -r "zap-$MODO.html" -J "zap-$MODO.json" -w "zap-$MODO.md"

node "$CARPETA/zap/sin-alertas-altas.mjs" "$CARPETA/resultados/zap/zap-$MODO.json"
