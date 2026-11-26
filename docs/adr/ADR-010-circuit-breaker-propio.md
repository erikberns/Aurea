# ADR-010: Circuit breaker propio para los sistemas externos
Estado: implementada.

## Contexto
El sistema depende de tres servicios externos: Stripe (REST), el proveedor mayorista (SOAP) y el modelo de IA. Todos tienen un timeout de 5 s, pero si un servicio está caído cada pedido esperaba esos 5 s antes de fallar, y la carga se acumulaba sobre un servicio que ya no respondía. La entrega exige timeout con fallback.

## Decisión
Una clase `CircuitBreaker` en `infraestructura/resiliencia`, sin dependencias, con tres estados: cerrado (deja pasar y cuenta fallas seguidas), abierto (rechaza al instante durante 30 s) y semiabierto (deja pasar una sola llamada de prueba). Se abre con 3 fallas seguidas. Cuenta como falla solo la falta de respuesta o un error técnico (timeout, conexión rechazada, 5xx); una respuesta de negocio (tarjeta rechazada, SOAP Fault, una respuesta de IA que no pasa la validación) cuenta como éxito, porque el sistema respondió.

Cada adapter usa su propia instancia y define su fallback: el pago responde 503 con Retry-After sin cobrar y conservando el carrito; la reposición queda SOLICITADA y se reintenta sin duplicarse; el asistente de IA devuelve la plantilla.

## Alternativas
Resilience4j es la opción estándar, pero agrega configuración y anotaciones que hay que conocer para explicarlas, y su integración con Spring Boot 4 había que verificarla. Una clase de unas 100 líneas, probada con un reloj controlado, alcanza para tres usos y cualquier integrante la puede explicar en la defensa.

## Consecuencias
Con un servicio caído, a partir de la cuarta llamada la respuesta es inmediata y explica cuándo reintentar. El estado vive en memoria: con varias instancias del backend, cada una tendría su propio circuito, algo aceptable para este alcance.
