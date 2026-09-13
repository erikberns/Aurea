# Áurea Joyas

Aplicación React + Spring Boot + JPA/PostgreSQL. Alcance de esta entrega: Usuarios, Carrito, Productos y Pedidos.

## Ejecución

Backend: JDK compatible (verificado con JDK 21) y Maven. Copiar `aurea-backend/.env.example` como `aurea-backend/.env` y completar los datos reales de PostgreSQL. `DB_URL` debe comenzar con `jdbc:postgresql://`; no colocar comillas alrededor de los valores, porque Spring importa el archivo como propiedades Java. `DDL_AUTO=validate` comprueba el esquema; `update` permite crearlo o actualizarlo en desarrollo. El lanzador de VS Code y Maven deben ejecutarse desde `aurea-backend` y usar ese mismo `.env`. Ejecutar desde `aurea-backend`:

```powershell
mvn spring-boot:run
```

Frontend: configurar `VITE_API_BASE_URL=http://localhost:8080/api` en su `.env`. Ejecutar desde `aurea-frontend`:

```powershell
npm install
npm run dev
```

Usar el mismo hostname en ambos lados y el origen autorizado por `CORS_ALLOWED_ORIGIN` (por defecto http://localhost:5173).

## Cuatro componentes, tres capas

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
    negocio/       IPedidos, ICheckout, ICalculoEnvio, implementaciones, DTO, eventos/ y strategy/
    datos/         Order, OrderItem, OrderStatus y OrderRepository
  infraestructura/
    configuracion/ Configuración de Spring Security
    presentacion/  Manejo transversal de errores HTTP
    datos/         Inicialización de datos y compatibilidad del esquema
```

Pedidos se denomina `ordenes` en el código. Categorías es parte de Productos y permite filtrar el catálogo. Infraestructura contiene soporte técnico compartido, no un quinto componente funcional.

La dirección entre capas es presentación → negocio → datos. Los controladores reciben interfaces de negocio por constructor y consumen DTO, sin acceder directamente a entidades o repositorios. El negocio accede a su propia capa de datos mediante interfaces. Para colaborar con otro componente utiliza sus contratos públicos de negocio; no importa sus servicios concretos, entidades ni repositorios. Los DTO no exponen entidades JPA y su conversión se realiza dentro del servicio propietario. El frontend React pertenece a presentación; sus clientes HTTP no reemplazan el negocio ni la persistencia del servidor.

| Componente | Contratos de negocio |
|---|---|
| Usuarios | IUsuarios; UserDetailsService para la integración con Spring Security |
| Carrito | ICarrito |
| Productos | IProductos |
| Pedidos | IPedidos, ICheckout e ICalculoEnvio |

Pedidos conserva los identificadores de usuario y producto y obtiene sus DTO mediante IUsuarios e IProductos. Las columnas existentes `user_id` y `product_id` mantienen sus nombres. OrderItem conserva el precio registrado al comprar. Los eventos de negocio son contratos inmutables: OrderConfirmedEvent permite comunicar la confirmación sin conocer al observador. La aplicación continúa siendo un único despliegue Spring Boot; la separación es lógica, por componentes y capas. La inicialización técnica de la base queda en infraestructura.

## Stateful y stateless

**Stateful:** `carrito/negocio/ServicioDeCarrito` usa `@Service` y `@SessionScope`. Spring le inyecta un ICarritoRepository cuya implementación tiene alcance prototype: cada instancia del servicio recibe un repositorio distinto, que almacena identificadores y cantidades en memoria, sin tabla SQL. El servicio conserva ese repositorio durante su sesión HTTP. Su callback de destrucción vacía la selección; Spring no ejecuta automáticamente la destrucción de los beans prototype.

El servicio aplica las reglas, consulta precios y stock en Productos y sincroniza las operaciones sobre su selección. La capa de datos encapsula guardar, consultar, quitar y vaciar. Al consultar devuelve una copia del mapa para evitar modificaciones externas.

`@PostConstruct` registra la creación. `@PreDestroy` vacía la selección y registra la destrucción. Cerrar sesión invalida el carrito. Una sesión que vence se elimina cuando el contenedor realiza su limpieza. No hay persistencia frente a un reinicio ni replicación entre servidores. Agregar no reserva stock.

El invitado puede seleccionar productos y conservarlos al autenticarse; se renueva el identificador de sesión. Cambiar a otra cuenta invalida la sesión anterior y no transfiere su carrito. Varias pestañas de la misma sesión comparten la selección.

**Stateless:** `productos/negocio/ProductService` atiende operaciones sin conservar usuario ni selección entre solicitudes. Es un singleton administrado por Spring, con callbacks de inicialización y destrucción. Singleton describe el alcance; stateless describe la ausencia de estado conversacional.

## Compra y patrones

El catálogo, la portada y el detalle utilizan la API real de productos. Todos los importes se calculan con el precio base del servidor y el costo de envío. El modelo y la API actuales no ofrecen descuentos.

`POST /api/carrito/checkout` toma la dirección y la selección de sesión. `CheckoutFacade.purchase` crea y confirma el pedido dentro de una transacción. Productos recibe el evento de confirmación y descuenta el stock en esa transacción. Si falla, se revierte el pedido y el carrito conserva su contenido; después del commit se vacía.

- **DAO:** ProductDAO y ProductDAOImpl encapsulan el acceso a productos.
- **Facade:** CheckoutFacade implementa ICheckout y coordina la compra mediante los contratos de Usuarios, Productos y Pedidos, además de su propio repositorio de pedidos.
- **Strategy:** CostoEnvio implementa ICalculoEnvio y recibe implementaciones de ShippingStrategy: envío gratuito desde 60.000 o estándar por 4.500.
- **Observer:** InventarioObserver recibe OrderConfirmedEvent y solicita el descuento de stock a IProductos. La entrega del evento es síncrona y participa en la transacción de confirmación.

No hay listener de notificaciones ni métricas de checkout. La confirmación no procesa un cobro externo.

## Seguridad y API

Spring Security autentica con sesión HTTP y cookie HttpOnly `JSESSIONID`; no se utiliza JWT. El cliente obtiene un token en `GET /api/csrf` antes de cada mutación y envía las credenciales de sesión. Las operaciones administrativas requieren ADMIN mediante `@PreAuthorize`.

| Método | Ruta | Operación |
|---|---|---|
| GET | /api/carrito | Consultar selección y totales |
| POST | /api/carrito/items | Agregar productoId y cantidad |
| DELETE | /api/carrito/items/{id} | Quitar producto |
| DELETE | /api/carrito | Vaciar selección |
| POST | /api/carrito/checkout | Comprar con direccion; exige autenticación |
| GET | /api/usuarios/sesion | Consultar perfil de sesión |
| POST | /api/usuarios/salir | Invalidar sesión |
| GET | /api/ordenes/mis-pedidos | Consultar historial propio |

## Demostración y pruebas

1. Abrir una ventana normal y otra de incógnito: agregar en una y comprobar que la otra tiene un carrito distinto.
2. Recargar y comprobar que la selección persiste; mostrar el callback de creación.
3. Iniciar sesión como invitado con artículos seleccionados y comprobar que se conservan.
4. Comprar y mostrar el pedido, el stock descontado y la bolsa vacía.
5. Agregar nuevamente y cerrar sesión: mostrar destrucción y nueva selección vacía.
6. Contrastar ServicioDeCarrito con ProductService y explicar las tres capas de cada componente.
7. Demostrar una operación administrativa rechazada a un cliente y permitida a ADMIN mediante un cliente HTTP.

```powershell
# Desde aurea-backend
mvn clean test
# Desde aurea-frontend
npm run build
npm run lint
```

Las 21 pruebas de backend usan H2 y MockMvc con los filtros de Spring Security. Cubren sesiones, compra, stock, validaciones y autorización, incluida la prohibición de confirmar pedidos ajenos y la reversión de pedido y stock si falla el observador. Las pruebas de arquitectura comprueban las tres capas de los cuatro componentes, prohíben referencias a datos o implementaciones de otros componentes, verifican la inyección mediante interfaces y revisan que las firmas de contratos y DTO no filtren entidades. Esto no sustituye el ensayo de navegador con PostgreSQL.

La eliminación de descuentos retira sus entidades y relaciones del código. No se ejecutaron borrados sobre PostgreSQL; en una base existente pueden permanecer tablas o columnas antiguas que Hibernate update no elimina. Los pedidos anteriores conservan sus importes registrados.
