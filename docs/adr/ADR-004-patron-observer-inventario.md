# ADR-004 · Patrón Observer y Mensajería Asincrónica para procesamiento post-confirmación

* **Estado:** Aceptada · 2026-10-20
* **Patrón:** Observer (GoF Comportamiento) + Mensajería Punto a Punto (EIP / AMQP)
* **Ubicación en el código:**
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/ordenes/negocio/eventos/OrderConfirmedEvent.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/productos/negocio/InventarioObserver.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/ordenes/negocio/OrderProcessingPublisher.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/ordenes/negocio/OrderProcessingListener.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/infraestructura/configuracion/RabbitConfiguration.java`

## Contexto
Al confirmarse un pedido (`Order.confirmar()`), ocurren dos tipos de reacciones con distintos requisitos de acoplamiento temporal:
1. **Descuento de stock en `Productos`:** Debe ocurrir de forma **sincrónica y atómica** dentro de la misma transacción de confirmación (si no alcanza el stock, debe revertirse la confirmación del pedido).
2. **Completado / procesamiento posterior de la orden:** Debe ejecutarse de forma **asincrónica** y desacoplada temporalmente mediante un broker de mensajería (RabbitMQ), garantizando que una tarea se procese una única vez (modelo Punto a Punto) solo después de que la transacción principal haya hecho `COMMIT`.

## Decisión
1. Publicar el evento inmutable de dominio `OrderConfirmedEvent` desde `OrderService.confirmOrder()`.
2. En el componente `Productos`, suscribir `InventarioObserver` (`@EventListener` sincrónico) para descontar stock dentro de la misma transacción sin que `Pedidos` conozca a `InventarioObserver`.
3. Suscribir `OrderProcessingPublisher` con `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` para enviar el ID del pedido confirmado al exchange `aurea.orders` y cola durable `aurea.orders.confirmed` en **RabbitMQ**.
4. Consumir la cola desde `OrderProcessingListener` (`@RabbitListener`), que invoca `IPedidos.completeOrder(orderId)` con comportamiento de **Receptor Idempotente**.

## Consecuencias
* **Positivas (+):**
  * Desacopla a `OrderService` de los efectos secundarios sincrónicos (inventario) y asincrónicos (cola RabbitMQ).
  * Usar `TransactionPhase.AFTER_COMMIT` evita publicar mensajes en RabbitMQ si la transacción de base de datos hace rollback.
  * La cola durable `aurea.orders.confirmed` retiene los mensajes si el consumidor se demora o reinicia.
* **Negativas (−):**
  * Introduce consistencia eventual para el paso a estado `COMPLETED` y requiere administrar el broker RabbitMQ e implementar idempotencia en el consumidor.

## Alternativas descartadas
* **Llamar a RabbitMQ antes del commit de la base de datos:** Descartado por riesgo de condición de carrera o publicación de pedidos cuya transacción luego fracasa.
* **Usar un tópico fanout sin cola durable para completar la orden:** Descartado porque completar el pedido es una tarea que debe ejecutarse una sola vez (modelo Punto a Punto / Queue).

