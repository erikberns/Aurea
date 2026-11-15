# ADR-008: Pasarela de pago REST (Stripe en modo de prueba) con autorización y captura
Estado: implementada.

## Contexto
La compra confirmaba pedidos sin cobro. Para demostrar cómo funcionaría en un entorno real se integra una pasarela de pago moderna vía REST. El checkout es un flujo crítico de varios pasos (crear pedido, cobrar, confirmar y descontar stock) protegido por una transacción declarativa de base de datos, pero esa transacción no puede deshacer una operación ya realizada en un sistema externo.

## Decisión
Crear el componente Pagos (presentación, negocio y datos) con el contrato IPagos y el puerto IPasarelaPago. StripePasarelaAdapter implementa el puerto contra la API HTTP de Stripe (POST /v1/payment_intents, form-urlencoded, autenticación Bearer, cabecera Idempotency-Key) sin usar el SDK, para que el contrato REST quede explícito. PasarelaSimuladaAdapter implementa el mismo puerto sin red y es la opción por defecto (PAGOS_PROVEEDOR=simulado) para pruebas y demostraciones sin conexión.

El cobro se resuelve en dos fases. Dentro de la transacción de CheckoutFacade.purchase se autoriza el pago con capture_method=manual, que solo retiene el importe. Pagos registra una sincronización de transacción: después del commit captura el cobro y, si la transacción se revierte, anula la retención. Un rechazo de tarjeta (HTTP 402 de Stripe) se traduce en PagoRechazadoException y revierte el pedido. Cada intento queda auditado en la tabla pago mediante transacciones REQUIRES_NEW, para que el registro sobreviva al rollback de la compra.

## Alternativas
Cobrar directamente (captura automática) obligaría a reembolsar cuando falla un paso posterior: es una compensación tipo saga, con un cobro visible para el cliente. Cobrar después del commit sin autorización previa confirmaría pedidos que luego podrían no pagarse. Usar el SDK oficial ocultaría el protocolo REST que se quiere demostrar. Recibir el número de tarjeta en el backend ampliaría el alcance PCI: en su lugar, el cliente elige un método de prueba de una lista blanca y, en producción, Stripe Elements tokenizaría la tarjeta en el navegador.

## Consecuencias
Una falla a mitad del flujo nunca deja un cobro sin pedido: la retención se anula y, si la anulación también falla, Stripe la vence automáticamente. Si la captura falla después del commit, el pago queda en CAPTURA_PENDIENTE y un administrador puede reintentarla (POST /api/pagos/{id}/capturar). La llamada a la pasarela ocurre dentro de la transacción de base de datos, acotada por un timeout de 5 s. Un timeout después de que Stripe autorizó deja una retención huérfana que vence sola; la clave de idempotencia por orden evita retenciones duplicadas en un reintento. Solo se aceptan claves de prueba (sk_test_); la clave secreta se configura por variable de entorno y nunca se versiona. La demostración con Stripe requiere conexión a internet.
