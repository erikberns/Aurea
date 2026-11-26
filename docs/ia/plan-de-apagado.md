# Plan de apagado del asistente de IA

## Cuándo apagarlo
- El modelo devuelve contenido inadecuado que el validador no detecta.
- Se agota la cuota del plan gratuito o el costo supera lo previsto.
- Un incidente de seguridad con la clave de API.
- Cualquier duda legal o de datos sobre el proveedor.

## Cómo apagarlo (sin tocar el código)
1. En el `.env`, poner `IA_HABILITADA=false`.
2. Reiniciar el backend: `docker compose up -d backend`.
3. Verificar en `GET /api/productos/sugerencia-ia/estado` que `habilitada` es `false`.

## Qué pasa con el sistema
El formulario de productos sigue funcionando igual: el asistente muestra que está apagado y la descripción se completa a mano. Si alguien llama al endpoint, recibe la plantilla de fallback y el aviso de que la IA está apagada. No se llama al modelo y la traza queda como `APAGADA`.

## Apagado automático parcial
Aunque esté encendido, el circuit breaker deja de llamar al modelo durante 30 s después de 3 fallas seguidas, y el timeout corta cualquier llamada a los 8 s.

## Si se filtró la clave
1. Revocarla en Google AI Studio.
2. Crear una nueva y cargarla en `IA_API_KEY`.
3. Reiniciar el backend.
