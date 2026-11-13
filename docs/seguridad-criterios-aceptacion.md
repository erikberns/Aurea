# Criterios de Aceptación de Seguridad Verificables (SEC-W / SEC-A / SEC-I)

Este documento explicita los criterios de aceptación de seguridad aplicables a la plataforma **Áurea**, alineados con la **Clase 11 (Seguridad como Criterio de Aceptación)**, y la evidencia técnica verificable en el código y la suite de pruebas automatizadas.

---

## 1. Web — Autenticación, Sesión y Transporte (`SEC-W`)

| ID | Criterio (`DADO / CUANDO / ENTONCES`) | Implementación en Código | Evidencia / Test Automatizado |
| :--- | :--- | :--- | :--- |
| **SEC-W01** | **DADO** que un usuario envía su contraseña en el registro o cambio de perfil, **CUANDO** se persiste en base de datos, **ENTONCES** se almacena con hash **BCrypt** con factor de trabajo `12` (`$2a$12$...`), nunca en texto plano. | `SecurityConfig.passwordEncoder()` (`new BCryptPasswordEncoder(12)`) y `UsuarioService`. | `SeguridadIntegracionTests.contrasenaSeAlmacenaConBcryptCost12YNoSeExponeEnApi()` |
| **SEC-W02** | **DADO** que un usuario se autentica o registra exitosamente, **CUANDO** el servidor emite credenciales de sesión y token, **ENTONCES** la cookie `AUREA_JWT` se emite con `HttpOnly`, `SameSite=Lax` y expiración acotada (`15 min`), y la sesión HTTP rota su identificador (`changeSessionId`) con banderas `HttpOnly` y `SameSite=Lax`. | `UsuarioController.adjuntarJwt()`, `SecurityConfig` y `application.properties`. | `SeguridadJwtYEstadoHttpTests`, `SeguridadIntegracionTests` |
| **SEC-W03** | **DADO** una petición mutante (`POST`, `PUT`, `PATCH`, `DELETE`) autenticada únicamente por cookie de sesión (sin `Authorization: Bearer`), **CUANDO** no incluye un token **CSRF** válido (`X-XSRF-TOKEN`), **ENTONCES** el servidor responde `403 Forbidden`. | `SecurityConfig.csrf()` con `CookieCsrfTokenRepository`. | `SeguridadIntegracionTests.csrfBloqueaMutacionesSinTokenYPermiteConToken()` |
| **SEC-W04** | **DADO** cualquier respuesta HTTP del backend, **CUANDO** el cliente la recibe, **ENTONCES** contiene las cabeceras de endurecimiento `Content-Security-Policy`, `X-Content-Type-Options: nosniff` y `X-Frame-Options: DENY`. | `SecurityConfig.headers()`. | `SeguridadIntegracionTests.respuestasIncluyenHeadersDeSeguridad()` |

---

## 2. API REST — JWT, Control de Acceso por Rol y BOLA/IDOR (`SEC-A`)

| ID | Criterio (`DADO / CUANDO / ENTONCES`) | Implementación en Código | Evidencia / Test Automatizado |
| :--- | :--- | :--- | :--- |
| **SEC-A01** | **DADO** un usuario autenticado con rol `USER` (vía JWT `Authorization: Bearer <token>` o sesión), **CUANDO** intenta consultar o cancelar una orden perteneciente a otro usuario (`GET /api/ordenes/{id}` o `POST /api/ordenes/{id}/cancelar`) o listar órdenes ajenas, **ENTONCES** el servidor responde `403 Forbidden` previniendo vulnerabilidades **BOLA / IDOR**. | `OrderController` y `OrderService.obtenerOrdenVerificandoAcceso()` / `cancelarOrden()`. | `SeguridadIntegracionTests.bolaImpideQueUnUsuarioAccedaAOtraOrden()`, `SeguridadJwtYEstadoHttpTests` |
| **SEC-A02** | **DADO** un cliente autenticado con rol `USER`, **CUANDO** intenta ejecutar operaciones administrativas (`POST/PUT/DELETE /api/productos/**` o `POST /api/ordenes/{id}/confirmar`), **ENTONCES** el servidor responde `403 Forbidden` mediante control de acceso por rol (`@PreAuthorize("hasRole('ADMIN')")` y reglas de ruta). | `SecurityConfig`, `ProductController`, `OrderController`. | `SeguridadJwtYEstadoHttpTests.autenticacionConBearerJwtControlaRolesRechazaAlgNoneYRevocaEnLogout()`, `SeguridadIntegracionTests` |
| **SEC-A03** | **DADO** un endpoint que retorna información de usuarios o de órdenes, **CUANDO** serializa la respuesta JSON, **ENTONCES** utiliza DTOs de salida (`UsuarioResponseDto`, `OrderSummaryDto`, `OrderDetailDto`, `ProductResponseDto`) y `@JsonProperty(access = WRITE_ONLY)` para no exponer jamás el hash de contraseña ni campos internos de persistencia. | `UsuarioResponseDto`, `OrderSummaryDto`, `OrderDetailDto`, `Usuario.contrasena`. | `SeguridadIntegracionTests.contrasenaSeAlmacenaConBcryptCost12YNoSeExponeEnApi()` |
| **SEC-A04** | **DADO** un endpoint que recibe payloads de entrada (`POST /api/usuarios/registro`, `POST /api/productos`, `POST /api/carrito/agregar`), **CUANDO** los datos violan restricciones de formato o rango (`@Valid`, `@Email`, `@NotBlank`, `@Positive`), **ENTONCES** el servidor rechaza la petición con `400 Bad Request` y detalle estructurado (`ErrorResponseDto`). | DTOs de entrada + `ApiExceptionHandler`. | `SeguridadIntegracionTests` |
| **SEC-A05** | **DADO** un token JWT con algoritmo `alg: "none"`, firma HMAC-SHA256 alterada, token expirado (`> 15 min`) o revocado tras `POST /api/usuarios/salir`, **CUANDO** se envía en `Authorization: Bearer <token>`, **ENTONCES** `JwtAuthenticationFilter` y `JwtService` rechazan la autenticación y el endpoint protegido responde `401 Unauthorized`. | `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig.logout()`. | `SeguridadJwtYEstadoHttpTests.autenticacionConBearerJwtControlaRolesRechazaAlgNoneYRevocaEnLogout()` |

---

## 3. Asíncrono e Integración — Mensajería y Secretos (`SEC-I`)

| ID | Criterio (`DADO / CUANDO / ENTONCES`) | Implementación en Código | Evidencia / Test Automatizado |
| :--- | :--- | :--- | :--- |
| **SEC-I01** | **DADO** un mensaje recibido en la cola durable `aurea.orders.confirmed`, **CUANDO** contiene una orden inexistente, un payload malformado o es re-entregado múltiples veces (entrega *at-least-once* de RabbitMQ), **ENTONCES** `OrderProcessingListener` procesa el mensaje de forma **idempotente** sin duplicar efectos ni corromper el estado `COMPLETED` de la orden. | `OrderProcessingListener`, `OrderService.completarOrdenConfirmada()`, `RabbitMqOrderMessagingConfig`. | `OrderMessagingIntegrationTests.procesaMensajeConfirmadoYEsIdempotenteAnteReentregas()` |
| **SEC-I02** | **DADO** el código fuente del repositorio, **CUANDO** se configuran credenciales de base de datos, broker AMQP o clave de firma JWT (`JWT_SECRET`), **ENTONCES** se inyectan mediante variables de entorno con valores por defecto únicamente para el entorno local de desarrollo (`application.properties` y `.env.example`). | `application.properties`, `.env.example`, `docker-compose.yml`. | Inspección de configuración y arranque de contexto en tests |

