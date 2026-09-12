# Áurea Joyas — Frontend (React)

Frontend del eCommerce **Áurea Joyas** (Grupo 8, Desarrollo de Aplicaciones II), construido en
**React + Vite + Tailwind CSS**, replicando el sistema de diseño "Modern Warmth Luxury" generado
en Stitch (paleta, tipografías Playfair Display / Plus Jakarta Sans, radios y espaciados).

Este frontend actúa como **cliente de la API REST** que expone el backend Spring Boot, tal como
está justificado en la documentación del TP: React se limita a la capa de presentación, y todas
las reglas de negocio (validaciones de registro, hashing de contraseñas, autorización por rol)
viven en `ServicioDeUsuarios`.

## Cómo correrlo

```bash
npm install
npm run dev
```

Abre en `http://localhost:5173`.

## Conexión con el backend

Por defecto el frontend corre en **modo mock** (`VITE_USE_MOCK=true`): simula
`ServicioDeUsuarios` con `localStorage`, para poder demostrar registro / login / edición de
perfil / cambio de contraseña sin tener el backend Spring Boot levantado.

Para conectarlo al backend real:

1. Copiá `.env.example` a `.env`.
2. Seteá `VITE_USE_MOCK=false`.
3. Seteá `VITE_API_BASE_URL` apuntando a tu backend (por defecto `http://localhost:8080/api`).
4. Implementá en Spring Boot los siguientes endpoints, que ya están contemplados en
   `src/services/usuariosService.js` (`realAdapter`):

   | Operación de iUsuarios      | Método y Path                          |
   |------------------------------|-----------------------------------------|
   | `registrarCliente`           | `POST /api/usuarios/registro`           |
   | `autenticar`                 | `POST /api/usuarios/autenticar`         |
   | `consultarPerfil`            | `GET  /api/usuarios/{id}`               |
   | `actualizarPerfil`           | `PUT  /api/usuarios/{id}`               |
   | `cambiarContrasena`          | `PATCH /api/usuarios/{id}/contrasena`   |
   | `asignarRol`                 | `PATCH /api/usuarios/{id}/rol`          |

   `autenticar` debe devolver `{ token, usuario }`, donde `token` es el JWT emitido por el
   backend. El resto de los endpoints (salvo registro y autenticación) requieren el header
   `Authorization: Bearer <token>`, que `httpClient.js` agrega automáticamente.

## Arquitectura del frontend (capas)

```
src/
  api/httpClient.js        -> capa de acceso a datos (fetch + JWT + manejo de errores)
  services/                -> capa de servicios (contrato iUsuarios / iCatalogo)
    usuariosService.js        - adapter real (REST) + adapter mock (localStorage)
    catalogoService.js        - datos de catálogo (stateless)
  context/                  -> estado de aplicación
    AuthContext.jsx            - sesión de usuario (análogo cliente de "stateless con JWT")
    CartContext.jsx            - carrito (análogo cliente de ServicioDeCarrito, stateful)
  components/               -> capa de presentación reutilizable (Header, Footer, cards, UI)
  pages/                     -> pantallas / rutas
```

Esta separación refleja, del lado del cliente, la arquitectura en capas exigida por la
consigna (presentación / negocio / datos), y el patrón **Adapter** aplicado en
`usuariosService.js`: las páginas siempre llaman al mismo contrato (`usuariosService.xxx`)
sin importar si detrás hay el backend real o el mock de desarrollo.

## Componentes Spring y ciclo de vida

El backend incluye componentes gestionados por el contenedor de Spring mediante anotaciones como
`@RestController`, `@Service`, `@Repository`, `@Component`, `@Configuration` y `@Bean`.

- `ProductService` es un componente **stateless**: no conserva estado propio entre invocaciones,
  delega persistencia en `ProductDAO` y valida su inicialización con `@PostConstruct`.
- `CheckoutMetricsState` es un componente **stateful**: conserva en memoria el contador de
  órdenes confirmadas, el último id confirmado y la fecha de confirmación durante la vida de la
  aplicación. Spring gestiona su ciclo de vida con `@PostConstruct` y `@PreDestroy`, y
  `OrderService` actualiza ese estado cuando una orden pasa a `CONFIRMED`.
- `NotificationService` también evidencia ciclo de vida gestionado por el contenedor: se
  inicializa con `@PostConstruct`, libera recursos lógicos con `@PreDestroy` y escucha eventos
  de dominio con `@EventListener`.

## Páginas incluidas

- **/** — Landing page oficial (réplica del mock de Stitch "Home Landing Page Oficial"): hero,
  categorías, piezas más deseadas, filosofía de marca, beneficios y testimonios. El header y el
  footer de esta pantalla se usan en todo el sitio, ya que es la referencia "oficial" de marca.
- **/catalogo** — catálogo de joyas con filtros (réplica del mock de Stitch "Catálogo").
- **/productos/:id** — ficha de producto.
- **/checkout** — bolsa / resumen de compra.
- **/login** y **/registro** — `iUsuarios.autenticar` y `iUsuarios.registrarCliente`.
- **/cuenta** (ruta protegida) — `consultarPerfil`, `actualizarPerfil`, `cambiarContrasena` y
  simulación de historial de pedidos.

## Pendiente para las próximas entregas

- Reemplazar `catalogoService.js` por llamadas reales a `ServicioDeCatalogo`.
- Conectar `CartContext` a `ServicioDeCarrito` / `ServicioDeInventario` (reserva temporal).
- Sumar manejo de expiración de JWT y refresco de sesión.