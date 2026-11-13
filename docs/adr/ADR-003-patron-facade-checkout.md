# ADR-003 · Patrón Facade para la coordinación del Checkout

* **Estado:** Aceptada · 2026-10-20
* **Patrón:** Facade (GoF Estructural) + Controlador / Indirección (GRASP)
* **Ubicación en el código:**
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/ordenes/negocio/ICheckout.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/ordenes/negocio/CheckoutFacade.java`

## Contexto
La operación de compra (`purchase` / `placeOrder`) requiere coordinar múltiples subsistemas y componentes independientes: consultar el perfil del comprador en `Usuarios` (`IUsuarios`), verificar vigencia, precio y disponibilidad de stock de cada joya en `Productos` (`IProductos`), calcular el costo de envío (`ICalculoEnvio`), persistir el pedido y confirmar la orden (`IPedidos`) dentro de una única frontera transaccional.

## Decisión
Implementar `CheckoutFacade` detrás del contrato `ICheckout`. Tanto el componente `Carrito` (`ServicioDeCarrito`) como el controlador `OrderController` invocan una única operación de alto nivel en `ICheckout`, delegando en la fachada la orquestación de los contratos públicos de `IUsuarios`, `IProductos`, `ICalculoEnvio` e `IPedidos`.

## Consecuencias
* **Positivas (+):**
  * Simplifica la interfaz para los consumidores (`ServicioDeCarrito` y `OrderController`), evitando que conozcan el orden interno de validación, tarificación y confirmación.
  * Mantiene el bajo acoplamiento entre componentes: `CheckoutFacade` solo depende de interfaces de negocio de los otros módulos, nunca de sus entidades JPA ni repositorios.
* **Negativas (−):**
  * Si se agregan demasiadas responsabilidades operativas directas en la fachada, existe riesgo de convertirla en una *God Class*; por eso las acciones posteriores a la confirmación se delegan mediante eventos (`OrderConfirmedEvent`).

## Alternativas descartadas
* **Orquestar la compra directamente desde `CarritoController` o `OrderController`:** Descartado porque violaría el principio de capa de presentación delgada y dispersaría la transacción de negocio.
* **Que `ServicioDeCarrito` invoque directamente a `OrderRepository`, `ProductDAO` y `UsuarioRepository`:** Descartado porque rompería el encapsulamiento entre componentes y las reglas de arquitectura en capas.

