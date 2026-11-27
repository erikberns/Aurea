#!/usr/bin/env sh
# Trivy (eje D: dependencias sin CVE críticas).
#   1) Dependencias: package-lock.json del frontend y las librerías dentro de los .jar del backend y del proveedor
#      (se leen los .jar y no los pom.xml para no depender de Maven Central, que limita las consultas con 429).
#      Antes, construir: aurea-backend y proveedor-soap con mvn package -DskipTests.
#      Se excluye pruebas/: son herramientas de prueba (Newman, Lighthouse) que no forman parte del sistema entregado.
#   2) Imágenes Docker construidas por docker compose (sistema operativo base + librerías dentro de la imagen).
# Falla si hay vulnerabilidades CRITICAL con corrección disponible. Las HIGH se informan.
set -u
# Git Bash (Windows) convierte rutas tipo /zap/wrk en C:/Program Files/Git/...: se desactiva y se usa la ruta de Windows.
export MSYS_NO_PATHCONV=1
CARPETA="$(cd "$(dirname "$0")/.." && (pwd -W 2>/dev/null || pwd))"
mkdir -p "$CARPETA/pruebas/resultados/trivy"
IMAGEN=aquasec/trivy:latest
CACHE="${TRIVY_CACHE:-trivy-cache}"
falla=0

echo "== Dependencias del frontend (package-lock.json) =="
docker run --rm -v "$CACHE:/root/.cache" -v "$CARPETA:/repo:ro" -v "$CARPETA/pruebas/resultados/trivy:/salida" $IMAGEN \
  fs --scanners vuln --severity HIGH,CRITICAL --ignore-unfixed --skip-dirs '**/node_modules' --skip-dirs pruebas \
  --skip-files '**/pom.xml' --format table --output /salida/dependencias-frontend.txt /repo
cat "$CARPETA/pruebas/resultados/trivy/dependencias-frontend.txt"
docker run --rm -v "$CACHE:/root/.cache" -v "$CARPETA:/repo:ro" $IMAGEN \
  fs --scanners vuln --severity CRITICAL --ignore-unfixed --exit-code 1 --quiet --skip-dirs '**/node_modules' \
  --skip-dirs pruebas --skip-files '**/pom.xml' /repo \
  || { echo "Hay CVE CRITICAL en las dependencias del frontend."; falla=1; }

# Los .jar se analizan en modo rootfs (el modo fs solo lee archivos de dependencias declaradas, no los .jar).
for modulo in aurea-backend proveedor-soap; do
  if ! ls "$CARPETA/$modulo/target/"*.jar > /dev/null 2>&1; then
    echo "Falta construir $modulo (mvn package -DskipTests): no se puede analizar su .jar."; falla=1; continue
  fi
  echo "== Librerías dentro del .jar de $modulo =="
  docker run --rm -v "$CACHE:/root/.cache" -v "$CARPETA/$modulo/target:/jar:ro" -v "$CARPETA/pruebas/resultados/trivy:/salida" $IMAGEN \
    rootfs --scanners vuln --severity HIGH,CRITICAL --ignore-unfixed --format table --output "/salida/jar-$modulo.txt" /jar
  cat "$CARPETA/pruebas/resultados/trivy/jar-$modulo.txt"
  docker run --rm -v "$CACHE:/root/.cache" -v "$CARPETA/$modulo/target:/jar:ro" $IMAGEN \
    rootfs --scanners vuln --severity CRITICAL --ignore-unfixed --exit-code 1 --quiet /jar \
    || { echo "Hay CVE CRITICAL en las librerías de $modulo."; falla=1; }
done

if [ "${SOLO_DEPENDENCIAS:-}" != "1" ]; then
  for img in aurea-backend aurea-frontend aurea-proveedor-soap; do
    echo "== Imagen $img =="
    docker run --rm -v "$CACHE:/root/.cache" -v /var/run/docker.sock:/var/run/docker.sock \
      -v "$CARPETA/pruebas/resultados/trivy:/salida" $IMAGEN \
      image --severity HIGH,CRITICAL --ignore-unfixed --format table --output "/salida/$img.txt" "$img"
    cat "$CARPETA/pruebas/resultados/trivy/$img.txt"
    docker run --rm -v "$CACHE:/root/.cache" -v /var/run/docker.sock:/var/run/docker.sock $IMAGEN \
      image --severity CRITICAL --ignore-unfixed --exit-code 1 --quiet "$img" \
      || { echo "Hay CVE CRITICAL en la imagen $img."; falla=1; }
  done
fi
exit $falla
