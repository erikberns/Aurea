# ADR-001 · Patrón State para el ciclo de vida de la Orden (`Order`)

* **Estado:** Aceptada · 2026-10-20
* **Patrón:** State (GoF Comportamiento)
* **Ubicación en el código:**
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/ordenes/datos/OrderState.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/ordenes/datos/TransicionEstadoInvalidaException.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/ordenes/datos/Order.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/ordenes/negocio/OrderService.java`

## Contexto
La entidad central de negocio `Order` atraviesa un ciclo de vida con estados bien definidos (`PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`). No todas las transiciones son válidas: por ejemplo, no se puede completar una orden aún `PENDING`, no se puede volver a confirmar una orden ya `CONFIRMED`, ni se puede realizar ninguna transición sobre una orden en estado final `CANCELLED`.
Validar estas reglas mediante condicionales `if/switch` dispersos en los servicios de negocio acopla la lógica de transición y dificulta agregar nuevos estados respetando el principio Abierto/Cerrado (OCP). Además, la API REST debe responder con código **HTTP 409 Conflict** ante cualquier intento de transición inválida.

## Decisión
Implementar el patrón **State** mediante la interfaz sellada `OrderState` (`sealed interface` con implementaciones `PendingState`, `ConfirmedState`, `CompletedState` y `CancelledState`):
1. La interfaz `OrderState` define las operaciones de transición (`confirmar()`, `completar()`, `cancelar()`) con métodos `default` que lanzan `TransicionEstadoInvalidaException` (subclase de `IllegalStateException`).
2. Cada estado concreto sobrescribe únicamente las transiciones permitidas desde ese estado y devuelve la nueva instancia de `OrderState`.
3. La entidad `Order` delega en su estado actual (`order.confirmar()`, `order.completar()`, `order.cancelar()`) y sincroniza el valor persistido en base de datos (`@Enumerated(EnumType.STRING) OrderStatus status`) reconstruyendo el objeto de estado mediante `@PostLoad`.
4. `ApiExceptionHandler` traduce `TransicionEstadoInvalidaException` / `IllegalStateException` en una respuesta **HTTP 409 Conflict**.

## Consecuencias
* **Positivas (+):**
  * Cada estado encapsula sus propias reglas de transición (Alta Cohesión y cumplimiento de OCP).
  * Imposibilidad de realizar transiciones inválidas sin que se dispare `TransicionEstadoInvalidaException` → `409 Conflict`.
  * Facilidad para testear unitariamente todas las combinaciones de transiciones válidas e inválidas (`OrderStateTests`).
* **Negativas (−):**
  * Mayor cantidad de clases/tipos para representar cada estado y necesidad de sincronizar el atributo `@Transient OrderState` con la columna `@Enumerated OrderStatus` de JPA.

## Alternativas descartadas
* **Condicionales `if (order.getStatus() != ...)` en `OrderService`:** Descartado porque dispersa las reglas del ciclo de vida fuera de la entidad experta y viola OCP cuando se agregan estados o transiciones.
* **Setters libres (`order.setStatus(...)`) desde la capa de servicio sin validación de máquina de estados:** Descartado porque permite transiciones inconsistentes (p. ej. cancelar una orden ya cancelada o completar una orden pendiente).

