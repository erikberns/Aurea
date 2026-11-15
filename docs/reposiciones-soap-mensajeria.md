# Reposición por SOAP y mensajería de recepción

Aurea incorpora Reposición y Notificaciones. El proveedor es una aplicación externa en la carpeta hermana `ProveedorSOAP/`, con su propio Maven y base de datos. La interfaz administrativa está en `/admin/reposiciones` y requiere ADMIN.

## Arquitectura

| Componente | Contrato | Responsabilidad |
|---|---|---|
| Usuarios | IUsuarios | Identidad y acceso |
| Productos | IProductos | Catálogo, stock y aplicación idempotente de recepciones |
| Carrito | ICarrito | Selección por sesión |
| Pedidos | IPedidos, ICheckout, ICalculoEnvio | Compra y procesamiento punto a punto |
| Reposición | IReposiciones, IProveedorGateway | Solicitud SOAP, seguimiento y recepción |
| Notificaciones | INotificaciones | Avisos administrativos persistentes |

Cada componente tiene presentación, negocio y datos. El Adapter SOAP implementa IProveedorGateway; no importa clases del proveedor. Construye mensajes SOAP 1.1 document/literal según el WSDL y valida las respuestas. El parser rechaza DTD y entidades externas.

El administrador elige un producto local y su código en el catálogo del proveedor. Esa asociación queda registrada por solicitud; no se comparten tablas ni se deduce del identificador local.

## Proceso síncrono

1. ADMIN consulta disponibilidad al proveedor por SOAP.
2. Aurea guarda SOLICITADA con un UUID antes de la llamada remota.
3. Consulta ese UUID remoto. Si no existe, envía solicitarReposicion con el mismo UUID.
4. Registra ACEPTADA o RECHAZADA. La aceptación no aumenta el stock de Aurea.
5. Ante timeout, SOAP Fault o respuesta inválida, conserva SOLICITADA y muestra el error. Reintentar consulta usa el mismo UUID.

El proveedor persiste solicitudes y rechaza reutilizar un UUID con datos distintos. Si se pierde una respuesta, consultarEstado recupera el resultado. Cada llamada tiene timeout de cinco segundos y no mantiene una transacción local abierta durante la espera.

## Proceso asincrónico

1. ADMIN registra la recepción física de una reposición ACEPTADA.
2. Una transacción marca RECIBIDA y guarda ReposicionRecibida en la outbox.
3. El publicador envía un mensaje persistente al exchange topic `aurea.reposiciones`, routing key `reposicion.recibida`.
4. RabbitMQ entrega una copia a `aurea.inventario.reposiciones` y otra a `aurea.notificaciones.reposiciones`.
5. Productos incrementa stock y registra eventId en una misma transacción.
6. Notificaciones guarda un aviso con eventId como clave única.

El aviso acredita recepción registrada, no que inventario ya terminó. La pantalla distingue inventario pendiente y actualizado.

Pedidos conserva su proceso punto a punto mediante el exchange directo `aurea.orders` y la cola `aurea.orders.confirmed`. El Observer local del checkout no se cuenta como publicación/suscripción con broker.

## Fallos y transacciones

- Repetir la recepción no crea otro evento. eventId coincide con el UUID de reposición.
- El evento contiene versión de esquema 1, producto, cantidad y fecha.
- Inventario bloquea el producto y comprueba eventId antes de incrementar. Stock y registro de procesamiento se confirman juntos.
- Notificaciones usa una clave primaria por evento; un reintento no duplica avisos.
- La outbox espera publisher confirm y comprueba devoluciones por falta de ruta. Si falla, reintenta con espera creciente hasta 60 segundos.
- Un fallo entre publicar y guardar la confirmación puede duplicar entregas. Los consumidores son idempotentes: no se promete exactly-once.
- Los consumidores reintentan hasta tres veces. Los mensajes que siguen fallando pasan a `aurea.inventario.reposiciones.dlq` o `aurea.notificaciones.reposiciones.dlq`.
- No hay transacción distribuida SOAP/PostgreSQL/RabbitMQ. Se usa atomicidad local y consistencia eventual.
- La outbox nueva cubre recepciones. El publicador anterior de pedidos sigue siendo AFTER_COMMIT sin outbox.

Las DLQ se inspeccionan en RabbitMQ Management. Corregir la causa antes de reenviar el payload a la cola original conservando eventId. No hay pantalla de reproceso. La outbox bloquea una fila durante la espera del confirm (hasta cinco segundos); es una solución acotada para este TP.

## API administrativa

Todos estos endpoints requieren ADMIN, además de autenticación y CSRF cuando se usan cookies.

| Método | Ruta | Uso |
|---|---|---|
| GET | /api/reposiciones | Seguimiento, publicación y estado de inventario |
| GET | /api/reposiciones/disponibilidad?codigo=ANILLO-PLATA | Disponibilidad SOAP |
| POST | /api/reposiciones | idSolicitud, productoId, codigoProveedor, cantidad; responde 202 |
| POST | /api/reposiciones/{id}/sincronizar | Consultar/reintentar resultado remoto |
| POST | /api/reposiciones/{id}/recibir | Registrar recepción física |
| GET | /api/notificaciones | Últimos 100 avisos |

Disponibilidad responde 503 si el proveedor falla. Transiciones inválidas responden 409 y entradas inválidas, 400. Una solicitud guardada devuelve SOLICITADA aunque el proveedor no conteste.

## Arranque local

1. En `ProveedorSOAP/`: `mvn package` y `java -jar target/proveedor-soap-1.0.0.jar`. WSDL: http://127.0.0.1:8091/soap/proveedor?wsdl.
2. En `aurea-backend/`, configurar PostgreSQL en .env. Crear las tablas nuevas con `docs/sql/002_reposiciones.sql` sobre la base elegida, o usar DDL_AUTO=update en desarrollo. No usar create-drop sobre datos reales.
3. Ejecutar `docker compose up -d rabbitmq`.
4. Configurar PROVEEDOR_SOAP_URL=http://127.0.0.1:8091/soap/proveedor. Ejecutar `mvn spring-boot:run`.
5. En `aurea-frontend/`: `npm run dev`. Entrar como ADMIN y abrir Reposiciones.

Maven debe usar Java 25. Si PostgreSQL rechaza el alias America/Buenos_Aires, iniciar la JVM con -Duser.timezone=America/Argentina/Buenos_Aires o UTC para pruebas.

## Demostración

Solicitar tres unidades y verificar que stock no cambia. Registrar recepción y observar el incremento y el aviso. Repetir recepción y reenviar el evento: no debe duplicar efectos. Detener el proveedor, solicitar, reiniciarlo y reintentar. Detener RabbitMQ antes de recibir: el evento queda pendiente y se procesa al reiniciar el broker.

El proveedor es una simulación académica local, no un proveedor comercial conectado. El aviso administrativo es persistente y no envía correos ni mensajes a terceros.

## Pruebas

`mvn test` ejecuta pruebas con H2 y proveedores controlados, sin RabbitMQ. Las pruebas del Adapter y del proveedor utilizan HTTP SOAP en puertos temporales.

`IntegracionRealTests` es opt-in: PostgreSQL y RabbitMQ en puertos aislados, proveedor separado en 18091. Ejecutar `scripts/verify-integrations.ps1` con Java 25, Maven y Docker. Inicia el entorno temporal, prueba los flujos y lo cierra, sin usar la base de .env.

Referencias técnicas: [Eclipse Metro](https://eclipse-ee4j.github.io/metro-jax-ws/) y [confirms y returns de Spring AMQP](https://docs.spring.io/spring-amqp/reference/amqp/template.html).
