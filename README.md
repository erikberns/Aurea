# Áurea Joyas

Aplicación React + Spring Boot + JPA/PostgreSQL. Componentes: Usuarios, Carrito, Productos, Pedidos, Pagos, Reposición y Notificaciones.

## Reposición SOAP y recepción asincrónica

La pantalla ADMIN `/admin/reposiciones` permite solicitar mercadería al proveedor SOAP externo y registrar su recepción. Una outbox publica ReposicionRecibida a RabbitMQ; inventario y avisos administrativos tienen suscripciones independientes. Aceptar la solicitud no aumenta stock: lo hace el consumidor al recibir el evento.

El proveedor vive en `proveedor-soap/`: está versionado en este repositorio pero es un proyecto independiente, con su propio Maven, WSDL, almacenamiento, proceso y contenedor. Ver [arranque, contratos y pruebas](docs/reposiciones-soap-mensajeria.md). Para bases con `DDL_AUTO=validate`, aplicar [las tablas nuevas](docs/sql/002_reposiciones.sql) antes de arrancar.

## Panel de administración

`/admin` reúne la gestión de la tienda en un único panel, accesible solo para usuarios con rol `ADMIN`. `ProtectedRoute` oculta las pantallas a otros usuarios, pero la autorización real la aplica el backend con `@PreAuthorize("hasRole('ADMIN')")` en cada endpoint. Un cliente que llame directamente a la API recibe `403`.

| Sección | Qué permite | API usada |
|---|---|---|
| Panel | Resumen: stock crítico con umbral ajustable, ventas del mes, reposiciones en curso y últimos avisos | Productos, pedidos, reposiciones y notificaciones |
| Productos | Catálogo con búsqueda y filtros; alta y edición de joyas; precio y stock editables en línea; archivar y reactivar | `POST/PUT /api/productos`, `PATCH .../precio`, `.../inventario`, `.../estado` |
| Pedidos | Pedidos de todos los clientes con el estado de su cobro | `GET /api/ordenes`, `GET /api/pagos` |
| Usuarios | Usuarios registrados y asignación del rol `ADMIN` (no permite cambiar el propio rol) | `GET /api/usuarios`, `PATCH /api/usuarios/{id}/rol` |
| Reposiciones | 1) Solicitud al proveedor SOAP con consulta de disponibilidad; 2) seguimiento y registro de la recepción física; 3) registro de ingresos acreditados | `/api/reposiciones/**` |
| Avisos | Bitácora de recepciones generada por el suscriptor del tópico | `GET /api/notificaciones` |

Archivar una joya la oculta de la tienda y del carrito sin borrarla, para no romper el historial de pedidos que la referencian. Para acceder, iniciar sesión con un usuario `ADMIN` (el `DataSeeder` crea `admin@aurea.com`); el enlace "Panel admin" aparece en la cabecera de la tienda.

## Pagos REST (Stripe en modo de prueba)

El checkout cobra mediante una pasarela de pago moderna vía REST. `StripePasarelaAdapter` llama a la API HTTP de Stripe (`POST /v1/payment_intents`, autenticación Bearer, `Idempotency-Key`) en modo de prueba; `PasarelaSimuladaAdapter` cumple el mismo contrato sin red y es la opción por defecto. El cliente elige una tarjeta de prueba (aprobada, rechazada o sin fondos): el backend nunca recibe números de tarjeta.

El pago se autoriza dentro de la transacción de compra y se captura después del commit; si algo falla a mitad del flujo, el pedido y el stock se revierten y la retención se anula. Ver [ADR-008](docs/adr/ADR-008-pasarela-pago-rest-stripe.md).

Para usar Stripe: crear una cuenta gratuita, copiar la clave secreta de prueba (Developers > API keys, modo Test) y configurar en `aurea-backend/.env`:

```properties
PAGOS_PROVEEDOR=stripe
STRIPE_SECRET_KEY=sk_test_...
```

Solo se aceptan claves `sk_test_`. Los pagos aparecen en el panel de Stripe (modo Test > Payments). Para bases con `DDL_AUTO=validate`, aplicar [la tabla de pagos](docs/sql/003_pagos.sql).

## Levantar el sistema completo con Docker

Es la forma recomendada para probar el proyecto sin instalar Java, Maven ni Node. Solo se necesita Docker Desktop (o Docker Engine con Compose v2). Un único comando levanta las seis piezas: PostgreSQL, RabbitMQ, el proveedor SOAP, el backend, el frontend y el proxy web.

1. Clonar el repositorio y entrar en la carpeta raíz `Aurea/`.
2. Copiar el archivo de ejemplo de variables:

   ```bash
   cp .env.example .env
   ```

   En Windows PowerShell: `Copy-Item .env.example .env`.
3. Abrir `.env` y completar estos valores (no usar comillas):

   | Variable | Qué poner |
   |---|---|
   | `DB_USERNAME` / `DB_PASSWORD` | Usuario y contraseña que se crearán en PostgreSQL. Pueden ser cualquiera. |
   | `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | Usuario y contraseña que se crearán en RabbitMQ. |
   | `JWT_SECRET` | Una frase cualquiera de **al menos 32 caracteres**. |
   | `ADMIN_PASSWORD` | Contraseña del administrador inicial. |
   | `ADMIN_EMAIL` | Opcional. Por defecto `admin@aurea.com`. |
   | `PAGOS_PROVEEDOR` | Dejar `simulado`. Para usar Stripe en modo de prueba, poner `stripe` y completar `STRIPE_SECRET_KEY` con una clave `sk_test_`. |
   | `IA_API_KEY` | Opcional. Sin clave, el asistente de IA usa un modelo simulado. Con una clave gratuita de Google AI Studio usa Gemini. |

4. Levantar todo:

   ```bash
   docker compose up --build
   ```

   La primera vez descarga imágenes y dependencias (unos minutos). Si falta el `.env` o alguna variable obligatoria, Compose se detiene antes de arrancar e indica cuál falta, por ejemplo `Falta JWT_SECRET en .env`. Si `JWT_SECRET` tiene menos de 32 caracteres, el backend no arranca y lo informa en su log.

Una vez levantado:

| Qué | Dirección |
|---|---|
| Tienda (frontend) | http://localhost:5173 |
| API REST | http://localhost:8080/api |
| WSDL del proveedor SOAP | http://localhost:8091/soap/proveedor?wsdl |
| Consola de RabbitMQ | http://localhost:15672 (usuario y contraseña de `.env`) |

Usuarios para ingresar:

- **Administrador:** el email de `ADMIN_EMAIL` (por defecto `admin@aurea.com`) con la contraseña que se puso en `ADMIN_PASSWORD`. Se crea solo en el primer arranque, con la base vacía.
- **Cliente:** se registra desde la tienda con "Crear cuenta".

El catálogo de joyas de ejemplo se carga automáticamente la primera vez. Para detener todo: `docker compose down`. Para borrar también los datos y empezar de cero: `docker compose down -v`.

El frontend se sirve con nginx, que reenvía `/api` al backend: navegador, cookies y API quedan en el mismo origen. Ninguna contraseña se muestra en los logs.

## Asistente de descripciones con IA

En el formulario de productos del panel (`/admin/productos`), el administrador carga el nombre, el material y los detalles de una joya y aprieta «Sugerir con IA». El asistente propone una descripción y una categoría; el administrador la revisa y decide si la usa. Nada se guarda sin su aprobación.

- **Sin clave** funciona con un modelo simulado, sin red: así levanta con `docker compose up` y corre en el CI.
- **Con Gemini:** completar `IA_API_KEY` en el `.env` con una clave gratuita de [Google AI Studio](https://aistudio.google.com/) y reiniciar el backend. No hace falta cambiar nada más. Si el modelo configurado en `IA_MODELO` ya no existe, copiar el identificador actual desde AI Studio.
- **Apagarlo:** `IA_HABILITADA=false`. El formulario sigue funcionando a mano.

El modelo queda detrás de un gateway con timeout de 5 s, circuit breaker, límite de 10 solicitudes por minuto y trazas sin datos personales (`GET /api/ia/trazas`). Si la IA falla o su respuesta no pasa las validaciones, se devuelve una plantilla para completar a mano. Diseño en [ADR-009](docs/adr/ADR-009-gateway-ia-descripciones.md); ficha, política de datos, riesgos OWASP y plan de apagado en [docs/ia](docs/ia); eval set en [aurea-backend/evals](aurea-backend/evals/README.md).

## Ejecución en modo desarrollo

Para trabajar sobre el código sin contenedores para backend y frontend.

Backend: JDK 25 (LTS), Maven y Docker Compose. Copiar `aurea-backend/.env.example` como `aurea-backend/.env` y completar los datos de PostgreSQL, RabbitMQ, `JWT_SECRET` y `ADMIN_PASSWORD`. `DB_URL` debe comenzar con `jdbc:postgresql://`; no colocar comillas alrededor de los valores, porque Spring importa el archivo como propiedades Java. `DDL_AUTO=validate` comprueba el esquema; `update` permite crearlo o actualizarlo en desarrollo. El lanzador de VS Code y Maven deben ejecutarse desde `aurea-backend` y usar ese mismo `.env`. Iniciar RabbitMQ desde `aurea-backend` con `docker compose up -d rabbitmq`, y luego:

```powershell
mvn spring-boot:run
```

Frontend: configurar `VITE_API_BASE_URL=http://localhost:8080/api` en su `.env`. Ejecutar desde `aurea-frontend`:

```powershell
npm install
npm run dev
```

Usar el mismo hostname en ambos lados y el origen autorizado por `CORS_ALLOWED_ORIGIN` (por defecto http://localhost:5173).

## Siete componentes, tres capas

```text
com/joyeriaEcommerce/AureaTPO/
  usuarios/
    presentacion/  UsuarioController, CsrfController y requests
    negocio/       IUsuarios, ServicioDeUsuarios, UsuarioDetailsService y DTO
    datos/         Usuario, Rol y UsuarioRepository
  carrito/
    presentacion/  CarritoController
    negocio/       ICarrito, ServicioDeCarrito y CarritoDTO
    datos/         ICarritoRepository y CarritoRepository (memoria)
  productos/
    presentacion/  ProductController y requests
    negocio/       IProductos, ProductService, InventarioObserver y DTO
    datos/         Product, ProductDAO, ProductDAOImpl, Category y CategoryRepository
  ordenes/
    presentacion/  OrderController y requests
    negocio/       IPedidos, ICheckout, ICalculoEnvio, implementaciones, DTO, publicador y consumidor RabbitMQ, eventos/ y strategy/
    datos/         Order, OrderItem, OrderStatus, OrderState (State Pattern), TransicionEstadoInvalidaException y OrderRepository
  pagos/
    presentacion/  PagoController (configuración de pago, auditoría y reintento de captura)
    negocio/       IPagos, IPasarelaPago, ServicioDePagos, StripePasarelaAdapter, PasarelaSimuladaAdapter y DTO
    datos/         Pago, EstadoPago y PagoRepository
  infraestructura/
    configuracion/ Configuración de Spring Security, JWT (IJwtService, JwtService, JwtAuthenticationFilter) y RabbitMQ
    presentacion/  Manejo transversal de errores HTTP (ApiExceptionHandler)
    datos/         Inicialización de datos y compatibilidad del esquema
  reposiciones/
    presentacion/  API administrativa de reposiciones
    negocio/       IReposiciones, IProveedorGateway, Adapter SOAP, evento y publicador outbox
    datos/         Reposicion, EventoPendiente y repositorios
  notificaciones/
    presentacion/  API administrativa de avisos
    negocio/       INotificaciones, servicio y consumidor
    datos/         Notificacion y repositorio
```

Pedidos se denomina `ordenes` en el código. Categorías es parte de Productos y permite filtrar el catálogo. Infraestructura contiene soporte técnico compartido, no un componente funcional adicional.

La dirección entre capas es presentación → negocio → datos. Los controladores reciben interfaces de negocio por constructor y consumen DTO, sin acceder directamente a entidades o repositorios. El negocio accede a su propia capa de datos mediante interfaces. Para colaborar con otro componente utiliza sus contratos públicos de negocio; no importa sus servicios concretos, entidades ni repositorios. Los DTO no exponen entidades JPA y su conversión se realiza dentro del servicio propietario. El frontend React pertenece a presentación; sus clientes HTTP no reemplazan el negocio ni la persistencia del servidor.

| Componente | Contratos de negocio |
|---|---|
| Usuarios | IUsuarios; UserDetailsService para la integración con Spring Security |
| Carrito | ICarrito |
| Productos | IProductos |
| Pedidos | IPedidos, ICheckout e ICalculoEnvio |
| Pagos | IPagos e IPasarelaPago |
| Reposición | IReposiciones e IProveedorGateway |
| Notificaciones | INotificaciones |

Pedidos conserva los identificadores de usuario y producto y obtiene sus DTO mediante IUsuarios e IProductos. Las columnas existentes `user_id` y `product_id` mantienen sus nombres. OrderItem conserva el precio registrado al comprar. Los eventos de negocio son contratos inmutables: OrderConfirmedEvent permite comunicar la confirmación sin conocer al observador. La aplicación continúa siendo un único despliegue Spring Boot; la separación es lógica, por componentes y capas. La inicialización técnica de la base queda en infraestructura.

## Stateful y stateless

**Stateful:** `carrito/negocio/ServicioDeCarrito` usa `@Service` y `@SessionScope`. Spring le inyecta un ICarritoRepository cuya implementación tiene alcance prototype: cada instancia del servicio recibe un repositorio distinto, que almacena identificadores y cantidades en memoria, sin tabla SQL. El servicio conserva ese repositorio durante su sesión HTTP. Su callback de destrucción vacía la selección; Spring no ejecuta automáticamente la destrucción de los beans prototype.

El servicio aplica las reglas, consulta precios y stock en Productos y sincroniza las operaciones sobre su selección. La capa de datos encapsula guardar, consultar, quitar y vaciar. Al consultar devuelve una copia del mapa para evitar modificaciones externas.

`@PostConstruct` registra la creación. `@PreDestroy` vacía la selección y registra la destrucción. Cerrar sesión invalida el carrito. Una sesión que vence se elimina cuando el contenedor realiza su limpieza. No hay persistencia frente a un reinicio ni replicación entre servidores. Agregar no reserva stock.

El invitado puede seleccionar productos y conservarlos al autenticarse; se renueva el identificador de sesión. Cambiar a otra cuenta invalida la sesión anterior y no transfiere su carrito. Varias pestañas de la misma sesión comparten la selección.

**Stateless:** `productos/negocio/ProductService` atiende operaciones sin conservar usuario ni selección entre solicitudes. Es un singleton administrado por Spring, con callbacks de inicialización y destrucción. Singleton describe el alcance; stateless describe la ausencia de estado conversacional.

## Compra y patrones

El catálogo, la portada y el detalle utilizan la API real de productos. Todos los importes se calculan con el precio base del servidor y el costo de envío. El modelo y la API actuales no ofrecen descuentos.

`POST /api/carrito/checkout` toma la dirección, el método de pago de prueba y la selección de sesión. `CheckoutFacade.purchase` crea el pedido, autoriza el pago y confirma el pedido dentro de una transacción declarativa. Productos recibe el evento de confirmación y descuenta el stock en esa transacción. Si cualquier paso falla (tarjeta rechazada, stock insuficiente), se revierte el pedido, Pagos anula la retención y el carrito conserva su contenido; después del commit se captura el cobro y se vacía el carrito.

- **State (con ADR-001):** `Order` delega su ciclo de vida (`PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`) a objetos polimórficos `OrderState` (`PendingState`, `ConfirmedState`, `CompletedState`, `CancelledState`). Cualquier transición inválida lanza `TransicionEstadoInvalidaException`, traducida a **HTTP 409 Conflict** por `ApiExceptionHandler`.
- **Strategy (con ADR-002):** `CostoEnvio` implementa `ICalculoEnvio` y selecciona polimórficamente mediante `ShippingStrategy.soporta(double cartTotal)` (cumpliendo OCP): envío gratuito desde 60.000 o estándar por 4.500.
- **Facade (con ADR-003):** `CheckoutFacade` implementa `ICheckout` y coordina la compra mediante los contratos de Usuarios, Productos, Pedidos y Pagos, además de su propio repositorio de pedidos.
- **Observer (con ADR-004):** `InventarioObserver` recibe `OrderConfirmedEvent` y solicita el descuento de stock a `IProductos`. La entrega del evento es síncrona y participa en la transacción de confirmación.
- **DAO (con ADR-005):** `ProductDAO` y `ProductDAOImpl` encapsulan el acceso a productos.
- **Mensajería asíncrona:** tras el commit de confirmación (`afterCommit`), `OrderProcessingPublisher` publica el evento en RabbitMQ. `OrderProcessingListener` consume la cola durable `aurea.orders.confirmed` y completa la orden mediante `IPedidos` (`order.completar()`). El consumidor es idempotente ante re-entregas; la cola y el exchange se declaran automáticamente.

Los Architecture Decision Records (ADRs) se encuentran en `docs/adr/` (`ADR-001` a `ADR-008`), y la matriz de criterios de aceptación de seguridad verificables en `docs/seguridad-criterios-aceptacion.md`.

Los avisos administrativos nuevos consumen recepciones de mercadería. No hay notificaciones ni métricas de checkout. El cobro se procesa en el entorno de prueba de la pasarela; no hay cobros reales.

## Seguridad y API

Spring Security soporta autenticación mediante **JWT (RFC 7519 HS256)** con expiración acotada a 15 minutos (`Authorization: Bearer <token>` y cookie `AUREA_JWT` `HttpOnly` / `SameSite=Lax`) emitidos en `POST /api/usuarios/autenticar`, en el registro (`POST /api/usuarios`) y en `GET /api/usuarios/token`, con validación constante de firma, rechazo explícito de `alg: "none"` (`401 Unauthorized`) y revocación en `/api/usuarios/salir`. Mantiene compatibilidad con la sesión HTTP (`JSESSIONID`) para preservar el carrito `@SessionScope` de invitados y usuarios autenticados. Las peticiones mutantes autenticadas únicamente por sesión exigen token CSRF (`GET /api/csrf`), mientras que las peticiones con `Authorization: Bearer <token>` se autentican por filtro JWT. Las operaciones administrativas requieren rol `ADMIN` mediante `@PreAuthorize("hasRole('ADMIN')")` y las órdenes verifican propiedad para prevenir BOLA/IDOR (`403 Forbidden`).

| Método | Ruta | Operación |
|---|---|---|
| GET | /api/carrito | Consultar selección y totales |
| POST | /api/carrito/items | Agregar productoId y cantidad |
| DELETE | /api/carrito/items/{id} | Quitar producto |
| DELETE | /api/carrito | Vaciar selección |
| POST | /api/carrito/checkout | Comprar con direccion y metodoPago; exige autenticación (402 si el pago es rechazado) |
| POST | /api/usuarios/autenticar | Autenticar usuario y emitir JWT (`Authorization: Bearer` + cookie `AUREA_JWT`) |
| GET | /api/usuarios/token | Emitir/renovar JWT para el usuario autenticado |
| GET | /api/usuarios/sesion | Consultar perfil autenticado |
| POST | /api/usuarios/salir | Revocar JWT e invalidar sesión |
| GET | /api/ordenes/mis-pedidos | Consultar historial propio |
| POST | /api/ordenes/{id}/confirmar | Confirmar orden (requiere `ADMIN`; 409 si la transición es inválida) |
| POST | /api/ordenes/{id}/cancelar | Cancelar orden (propietario o `ADMIN`; 409 si ya fue completada o cancelada) |
| GET | /api/productos/categorias | Categorías del catálogo (público) |
| PATCH | /api/productos/{id}/estado?activo= | Archivar o reactivar una joya (requiere `ADMIN`) |
| GET | /api/pagos/configuracion | Pasarela activa y tarjetas de prueba disponibles; exige autenticación |
| GET | /api/pagos | Auditoría de intentos de pago (requiere `ADMIN`) |
| POST | /api/pagos/{id}/capturar | Reintentar una captura pendiente (requiere `ADMIN`) |

## Demostración y pruebas

1. Abrir una ventana normal y otra de incógnito: agregar en una y comprobar que la otra tiene un carrito distinto.
2. Recargar y comprobar que la selección persiste; mostrar el callback de creación.
3. Iniciar sesión como invitado con artículos seleccionados y comprobar que se conservan.
4. Comprar con una tarjeta rechazada y mostrar el 402, que no se creó el pedido y que la bolsa se conserva. Comprar con una tarjeta aprobada y mostrar el pedido, el stock descontado, la bolsa vacía y el pago capturado (en el panel de Stripe, modo Test, si se usa Stripe).
5. Agregar nuevamente y cerrar sesión: mostrar destrucción y nueva selección vacía.
6. Contrastar ServicioDeCarrito con ProductService y explicar las tres capas de cada componente.
7. Demostrar una operación administrativa rechazada a un cliente y permitida a ADMIN mediante un cliente HTTP, y que un cliente que navega a `/admin` es redirigido.
8. Desde el panel: crear una joya, bajarle el stock hasta el umbral crítico, solicitar su reposición al proveedor SOAP, registrar la recepción y ver el stock acreditado y el aviso generado.

```powershell
# Desde aurea-backend
mvn clean test
# Desde aurea-frontend
npm run build
npm run lint
```

La suite de backend contiene 60 pruebas: 58 se ejecutan sin servicios externos y 2 (`IntegracionRealTests`) requieren el entorno de integración real. Las pruebas habituales usan H2 y MockMvc con los filtros de Spring Security y JWT. Cubren el patrón State en `Order` (`OrderStateTests`), el patrón Strategy en `CostoEnvio` (`CostoEnvioStrategyTests`), la mensajería punto a punto e idempotencia (`OrderProcessingMessagingTests`), seguridad JWT (`Bearer`, rechazo de `alg: "none"`, revocación en logout, RBAC `403`) y `409 Conflict` ante transiciones inválidas (`SeguridadJwtYEstadoHttpTests`), sesiones, CSRF y compra (`CarritoHttpTests`), el pago transaccional con autorización, captura, rechazo `402` y anulación ante una falla a mitad del flujo (`PagosCheckoutTests`), el contrato REST con Stripe contra un servidor local (`StripePasarelaAdapterTests`), el Adapter SOAP y la reposición con outbox (`ProveedorSoapAdapterTests`, `ReposicionTests`), usuarios (`ServicioDeUsuariosTests`), la gestión de productos restringida a `ADMIN` (`ProductosAdminHttpTests`) y las reglas de arquitectura en capas (`ArquitecturaCapasTests`). Esto no sustituye el ensayo de navegador con PostgreSQL.

La eliminación de descuentos retira sus entidades y relaciones del código. No se ejecutaron borrados sobre PostgreSQL; en una base existente pueden permanecer tablas o columnas antiguas que Hibernate update no elimina. Los pedidos anteriores conservan sus importes registrados.
