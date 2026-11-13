# ADR-002 · Patrón Strategy para el cálculo de costo de envío

* **Estado:** Aceptada · 2026-10-20
* **Patrón:** Strategy (GoF Comportamiento) + OCP / DIP (SOLID)
* **Ubicación en el código:**
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/ordenes/negocio/ICalculoEnvio.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/ordenes/negocio/strategy/ShippingStrategy.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/ordenes/negocio/strategy/FreeShippingStrategy.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/ordenes/negocio/strategy/StandardShippingStrategy.java`
  * `aurea-backend/src/main/java/com/joyeriaEcommerce/AureaTPO/ordenes/negocio/strategy/CostoEnvio.java`

## Contexto
Durante el proceso de checkout de Áurea Joyas, el costo de envío depende de reglas comerciales que varían según el monto subtotal de la compra (actualmente envío bonificado para compras mayores o iguales a $60.000 y tarifa estándar de $4.500 para montos inferiores), y el negocio prevé incorporar nuevas modalidades (envío express, retiro en sucursal o tarifas por zona).

## Decisión
Aplicar el patrón **Strategy** definiendo la interfaz `ShippingStrategy` con los métodos `boolean soporta(double cartTotal)` y `double calculateShippingCost(double cartTotal)`, implementada por `FreeShippingStrategy` y `StandardShippingStrategy`. El contexto `CostoEnvio` (que expone el contrato de negocio `ICalculoEnvio`) selecciona polimórficamente la estrategia aplicable sin acoplar a `CheckoutFacade` a las reglas concretas de tarificación.

## Consecuencias
* **Positivas (+):**
  * Agregar una nueva regla de costo de envío implica crear una nueva clase que implemente `ShippingStrategy`, manteniendo cerrado a modificación el servicio de checkout (OCP).
  * Cada regla de cálculo puede probarse de forma aislada en tests unitarios (`CostoEnvioStrategyTests`).
* **Negativas (−):**
  * Incrementa el número de clases e interfaces para reglas de negocio simples.

## Alternativas descartadas
* **Cálculo directo con condicionales dentro de `CheckoutFacade`:** Descartado porque acopla la coordinación del checkout con los detalles tarifarios del envío (viola SRP y OCP).

