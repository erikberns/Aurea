# ADR-005 · Patrón DAO yVariaciones Protegidas en la capa de datos

* **Estado:** Aceptada · 2026-10-20
* **Patrón:** Data Access Object (DAO - Core J2EE / PoEAA) + Fabricación Pura / Variaciones Protegidas (GRASP)
* **Ubicación en el código:**
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/productos/datos/ProductDAO.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/productos/datos/ProductDAOImpl.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/carrito/datos/ICarritoRepository.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/carrito/datos/CarritoRepository.java`

## Contexto
La capa de negocio de `Productos` (`ProductService`) y de `Carrito` (`ServicioDeCarrito`) necesita abstraerse del mecanismo concreto de persistencia (PostgreSQL vía JPA/Hibernate en `Productos`, y almacenamiento en memoria por sesión HTTP en `Carrito`), respetando la regla arquitectónica de inyección por interfaz.

## Decisión
Encapsular el acceso a datos de productos detrás de la interfaz `ProductDAO` y su implementación `ProductDAOImpl` (`@Repository`), y el estado del carrito detrás de `ICarritoRepository` y `CarritoRepository` (`@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)`). `ArquitecturaCapasTests` verifica mediante reflexión que ningún servicio ni controlador dependa de implementaciones concretas de datos.

## Consecuencias
* **Positivas (+):**
  * Aísla a la capa de negocio de los detalles técnicos de JPA/SQL o de estructuras en memoria.
  * Permite reemplazar o mockear la capa de persistencia en pruebas unitarias sin levantar la base de datos.
* **Negativas (−):**
  * Añade una interfaz y clase adaptadora adicional en la capa de datos.

## Alternativas descartadas
* **Inyectar `EntityManager` directamente en `ProductService`:** Descartado porque acopla la lógica de negocio a la API de JPA.

