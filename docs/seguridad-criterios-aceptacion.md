# Criterios de aceptación de seguridad verificables (SEC-W / SEC-A / SEC-I)

Criterios de seguridad de **Áurea** en formato DADO / CUANDO / ENTONCES (Clase 11), con la implementación y la evidencia que los verifica. "Inspección" indica que el criterio se verifica revisando la configuración, sin prueba automatizada.

## 1. Web: autenticación, sesión y transporte (SEC-W)

| ID | Criterio | Implementación | Evidencia |
| :--- | :--- | :--- | :--- |
| **SEC-W01** | **DADO** un registro o cambio de contraseña, **CUANDO** se persiste, **ENTONCES** se guarda con hash BCrypt de costo 12, nunca en texto plano. | `ApplicationConfig.passwordEncoder()` (`BCryptPasswordEncoder(12)`), `ServicioDeUsuarios` | `ServicioDeUsuariosTests.registraUnClienteConEmailNormalizadoYContrasenaHasheada()` + inspección del costo |
| **SEC-W02** | **DADO** un usuario que se autentica o registra, **CUANDO** el servidor emite credenciales, **ENTONCES** entrega un JWT de 15 minutos en `Authorization: Bearer` y en la cookie `AUREA_JWT` (`HttpOnly`, `SameSite=Lax`), y rota el identificador de sesión. | `UsuarioController` (emisión del JWT y `changeSessionId()`), `JwtService` | `CarritoHttpTests.usaTokenCsrfRealYConservaCarritoInvitadoAlAutenticar()` (verifica la rotación de sesión) |
| **SEC-W03** | **DADO** una petición mutante autenticada solo por cookie de sesión, **CUANDO** no trae token CSRF válido, **ENTONCES** responde 403. | `SecurityConfig.csrf()` con `CookieCsrfTokenRepository` | `CarritoHttpTests.mutacionSinCsrfSeRechaza()` |
| **SEC-W04** | **DADO** un usuario autenticado, **CUANDO** cierra sesión, **ENTONCES** se revoca su JWT, se invalida la sesión y se destruye su carrito. | `SecurityConfig.logout()`, `JwtService.revokeToken()` | `CarritoHttpTests.logoutDestruyeEstadoYCancelaAutenticacion()`, `SeguridadJwtYEstadoHttpTests.autenticacionJwtValidaRechazaTokenAlteradoAlgNoneYRevocaEnLogout()` |
| **SEC-W05** | **DADO** cualquier respuesta del backend, **CUANDO** llega al navegador, **ENTONCES** incluye `X-Content-Type-Options: nosniff` y `X-Frame-Options: DENY`. | Cabeceras por defecto de Spring Security | Inspección |

## 2. API REST: JWT, roles y propiedad de los recursos (SEC-A)

| ID | Criterio | Implementación | Evidencia |
| :--- | :--- | :--- | :--- |
| **SEC-A01** | **DADO** un cliente autenticado, **CUANDO** intenta confirmar o listar un pedido ajeno, **ENTONCES** responde 403 (prevención BOLA/IDOR). | `OrderService.confirmOrder()` (verifica propiedad), `GET /api/ordenes` solo `ADMIN` | `CarritoHttpTests.otroUsuarioNoPuedeConfirmarNiListarUnPedidoAjeno()` |
| **SEC-A02** | **DADO** un usuario con rol `CLIENTE`, **CUANDO** ejecuta una operación administrativa (modificar productos, listar pedidos o pagos), **ENTONCES** responde 403; sin autenticación, 401. | `@PreAuthorize("hasRole('ADMIN')")` en `ProductController`, `OrderController`, `PagoController`, `ReposicionController`, `NotificacionController`, `UsuarioController` | `CarritoHttpTests.rechazaCantidadesInvalidasYExigeRolAdministrador()`, `ProductosAdminHttpTests`, `PagosCheckoutTests.metodoDesconocidoEsRechazadoYLaAuditoriaRequiereAdministrador()` |
| **SEC-A03** | **DADO** un endpoint que devuelve usuarios, pedidos o productos, **CUANDO** serializa la respuesta, **ENTONCES** usa DTO de negocio y nunca expone el hash de la contraseña ni entidades JPA. | `UsuarioDTO`, `OrderDTO`, `ProductDTO`, `PagoDTO` (records) | `ArquitecturaCapasTests.interfacesYDtoNoExponenEntidadesNiImplementaciones()` |
| **SEC-A04** | **DADO** un payload de entrada inválido (cantidades negativas, email mal formado, campos vacíos), **CUANDO** llega a la API, **ENTONCES** responde 400 con el detalle de los campos. | Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@Positive`) + `ApiExceptionHandler` | `CarritoHttpTests.rechazaCantidadesInvalidasYExigeRolAdministrador()` |
| **SEC-A05** | **DADO** un JWT con `alg: "none"`, firma alterada, vencido o revocado, **CUANDO** se envía como Bearer, **ENTONCES** responde 401. | `JwtService`, `JwtAuthenticationFilter` | `SeguridadJwtYEstadoHttpTests.autenticacionJwtValidaRechazaTokenAlteradoAlgNoneYRevocaEnLogout()` |
| **SEC-A06** | **DADO** un pedido en un estado que no admite la operación, **CUANDO** se intenta la transición (p. ej. confirmar dos veces), **ENTONCES** responde 409 sin modificar el pedido. | Patrón State (`OrderState`, `TransicionEstadoInvalidaException`) + `ApiExceptionHandler` | `SeguridadJwtYEstadoHttpTests.transicionInvalidaEnOrdenDevuelve409Conflict()`, `OrderStateTests` |
| **SEC-A07** | **DADO** un cliente que paga, **CUANDO** envía el checkout, **ENTONCES** solo envía un código de método de prueba de una lista blanca (nunca datos de tarjeta), y el backend rechaza claves de Stripe que no sean de prueba. | `MetodoPagoPrueba`, `StripePasarelaAdapter` | `PagosCheckoutTests.metodoDesconocidoEsRechazadoYLaAuditoriaRequiereAdministrador()`, `StripePasarelaAdapterTests.rechazaClavesProductivas()` |

## 3. Integración y mensajería (SEC-I)

| ID | Criterio | Implementación | Evidencia |
| :--- | :--- | :--- | :--- |
| **SEC-I01** | **DADO** un evento de recepción entregado más de una vez (entrega *at-least-once*), **CUANDO** lo procesan Inventario y Avisos, **ENTONCES** el stock y el aviso se aplican una sola vez por `eventId`. | `ProductService.aplicarReposicion()`, `NotificacionService`, colas con DLQ | `ReposicionTests.recibirGuardaOutboxSinModificarStockYAmbosConsumidoresSonIdempotentes()`, `ReposicionTests.inventarioConcurrenteAplicaEventoUnaSolaVez()` |
| **SEC-I02** | **DADO** el código del repositorio, **CUANDO** se configuran credenciales de base de datos, broker, clave JWT o Stripe, **ENTONCES** se inyectan por variables de entorno o `.env` (excluido de Git); los valores por defecto sirven solo para desarrollo local. | `application.properties` (`DB_*`, `RABBITMQ_*`, `JWT_SECRET`, `STRIPE_SECRET_KEY`), `.env.example`, `.gitignore` | Inspección |
| **SEC-I03** | **DADO** una respuesta del proveedor SOAP, **CUANDO** se procesa el XML, **ENTONCES** el parser rechaza DTD y entidades externas (XXE) y descarta respuestas fuera de contrato o de más de 64 KB. | `ProveedorSoapAdapter` | `ProveedorSoapAdapterTests.rechazaDtdYEntidadesExternas()`, `ProveedorSoapAdapterTests.traduceSoapFaultSinConfundirloConAceptacion()` |
| **SEC-I04** | **DADO** que el proveedor SOAP o Stripe no responden, **CUANDO** vence el timeout de 5 s, **ENTONCES** la operación falla con 503 sin dejar efectos parciales (la reposición queda `SOLICITADA`; la compra se revierte). | `ProveedorSoapAdapter`, `StripePasarelaAdapter`, `CheckoutFacade` (`@Transactional`) | `StripePasarelaAdapterTests.unaPasarelaCaidaIndicaPasarelaNoDisponible()`, `ReposicionTests.timeoutSeRecuperaConsultandoMismoIdentificador()` |
