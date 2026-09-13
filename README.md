# Áurea Joyas

Aplicación React + Spring Boot + JPA/PostgreSQL. El navegador consume la API real: no hay autenticación ni carrito simulado en localStorage.

## Ejecución local

Backend: usar un JDK compatible (verificado con JDK 21) y Maven/Maven Wrapper. Configurar las variables que referencia `aurea-backend/src/main/resources/application.properties`, incluyendo conexión, usuario y contraseña de PostgreSQL y `DDL_AUTO`. Ejecutar desde `aurea-backend`:

```powershell
mvn spring-boot:run
```

Frontend: copiar `.env.example` a `.env`, configurar `VITE_API_BASE_URL=http://localhost:8080/api` y ejecutar desde `aurea-frontend`:

```powershell
npm install
npm run dev
```

Usar el mismo hostname en ambos lados (por ejemplo localhost) y el origen autorizado por `CORS_ALLOWED_ORIGIN`. La sesión viaja en una cookie HttpOnly `JSESSIONID`. El cliente obtiene el token con `GET /api/csrf` antes de cada mutación. No se utiliza JWT.

## Stateful y stateless para la primera entrega

**Stateful:** `carrito/negocio/ServicioDeCarrito` implementa `ICarrito` y usa `@Service` + `@SessionScope`. Spring crea una instancia por sesión HTTP cuando se consulta por primera vez. Su mapa conserva identificadores y cantidades entre solicitudes. Los métodos sincronizados evitan modificaciones simultáneas sobre la misma instancia. Cada consulta obtiene precios actuales del servidor. Las sesiones diferentes tienen selecciones independientes.

`@PostConstruct` registra la creación y `@PreDestroy` vacía el mapa y registra la destrucción. El logout invalida la sesión y destruye el bean; el vencimiento de sesión también dispara su destrucción cuando el contenedor elimina la sesión. Recargar la página conserva el carrito mientras la cookie y la sesión sigan vigentes. Varias pestañas del mismo navegador comparten sesión y carrito. El estado es temporal: no se conserva al reiniciar el backend ni se replica entre instancias.

El carrito permite selección como invitado y la conserva al iniciar sesión; el servidor cambia el identificador de sesión. Si se autentica una cuenta distinta desde una sesión ya autenticada, se invalida la anterior. Comprar exige autenticación. Agregar no reserva inventario.

**Stateless:** `productos/negocio/ProductService` no conserva selección, usuario ni datos de solicitudes anteriores en sus atributos. Spring lo administra como singleton y le inyecta sus repositorios. Singleton describe el alcance de instancia; stateless describe su comportamiento. Tiene callbacks de inicialización y destrucción. `CostoEnvio` es otro ejemplo de cálculo sin estado conversacional.

## Capas y patrones

- Presentación: React y controladores REST, incluyendo `CarritoController`.
- Negocio: `ServicioDeCarrito`, `ProductService`, `OrderService`, `CheckoutFacade`.
- Datos: `ProductDAO`/`ProductDAOImpl`, repositorios JPA y entidades. El carrito utiliza el acceso existente a Productos; su selección temporal vive en el bean de sesión, sin una tabla artificial de carrito.
- DAO: acceso a productos encapsulado tras `ProductDAO`.
- Facade: `CheckoutFacade` coordina la compra.
- Strategy: `CostoEnvio` selecciona envío gratuito o estándar mediante `ShippingStrategy`.
- Observer: confirmación de pedidos publica un evento. El stock participa en la transacción; métricas y notificación simulada se procesan después del commit. Hay un único listener de notificaciones.

## API del carrito

| Método | Ruta | Operación |
|---|---|---|
| GET | `/api/carrito` | Consultar selección y totales calculados por el servidor |
| POST | `/api/carrito/items` | Agregar `{ "productoId": 1, "cantidad": 1 }` |
| DELETE | `/api/carrito/items/{id}` | Quitar un producto |
| DELETE | `/api/carrito` | Vaciar selección |
| POST | `/api/carrito/checkout` | Comprar con `{ "direccion": "Calle 123" }`; requiere sesión autenticada |
| GET | `/api/usuarios/sesion` | Perfil autenticado o respuesta vacía para invitado |
| POST | `/api/usuarios/salir` | Invalidar sesión y destruir carrito |

Todas las mutaciones requieren CSRF. `POST /api/carrito/checkout` usa los ítems del servidor. Crea y confirma la orden dentro de una transacción. Solo después de que esa operación retorna con commit se vacía la selección. Ante stock insuficiente o error de persistencia, se conserva el carrito. La confirmación no realiza un cobro externo.

## Guion de demostración

1. Iniciar backend y frontend; abrir un navegador normal (A) y uno incógnito (B).
2. En A agregar dos unidades de una joya. Recargar: permanecen. Observar el log de creación del carrito.
3. En B consultar la bolsa: está vacía. Agregar otro producto: A permanece sin cambios. Cada sesión registra una instancia de carrito diferente.
4. En A registrarse o iniciar sesión. La selección de invitado se conserva; consultar la cuenta acredita la sesión real.
5. Confirmar un pedido: mostrar el total devuelto, el historial y el stock descontado. La bolsa queda vacía.
6. Volver a agregar un producto y cerrar sesión: observar el log `Carrito ... destruido`. Al consultar una nueva bolsa, está vacía. B conserva su carrito.
7. Mostrar en código `@SessionScope`, el mapa y ambos callbacks. Comparar con `ProductService`: dependencias compartidas, sin estado conversacional.
8. Mostrar una operación administrativa: un cliente recibe 403; un administrador puede ejecutarla. Es posible demostrar los endpoints administrativos con un cliente HTTP; no existe un panel administrativo completo.

Para mostrar vencimiento, configurar temporalmente `server.servlet.session.timeout` y esperar la limpieza de sesiones del servidor. El timeout se mide desde la última actividad; la destrucción por expiración puede demorarse hasta la siguiente limpieza del contenedor.

## Validación

```powershell
# Backend: pruebas con H2, sin tocar PostgreSQL
mvn test
# Frontend
npm run build
npm run lint
```

`CarritoHttpTests` cubre aislamiento de sesiones, persistencia entre peticiones, destrucción por logout, token CSRF real, conservación del carrito invitado al autenticar, compra, stock insuficiente, cantidades inválidas y autorización de administrador. Las pruebas HTTP usan el contexto y los filtros reales de Spring con MockMvc.

## Alcance pendiente

Pagos y correos externos, reservas de inventario con vencimiento, SOAP, broker, colas y tópicos quedan para siguientes etapas. No se presentan los mensajes de consola como correos enviados. `CheckoutMetricsState` es una métrica global, no el ejemplo stateful principal. Los pedidos y productos siguen compartiendo el backend y la base de datos.
