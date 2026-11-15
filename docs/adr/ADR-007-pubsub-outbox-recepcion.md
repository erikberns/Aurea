# ADR-007: Publicación/suscripción y outbox de recepción
Estado: implementada.

## Contexto
Recibir mercadería debe actualizar inventario y generar un aviso, sin acoplar ambos consumidores. RabbitMQ puede estar caído cuando se registra la recepción y puede entregar mensajes repetidos.

## Decisión
Guardar RECIBIDA y el evento en una transacción PostgreSQL. Publicar mediante outbox con confirmación del broker en un exchange topic, con una cola durable por consumidor y una DLQ por cola. Inventario registra eventId y aumenta stock atómicamente; Notificaciones usa eventId como clave única.

## Alternativas
Compartir una cola repartiría trabajo, sin entregar una copia a ambos consumidores. Publicar solamente AFTER_COMMIT podría perder el evento si el broker falla. Una transacción distribuida agregaría complejidad y no elimina la necesidad de idempotencia.

## Consecuencias
Consistencia eventual y entrega al menos una vez. Se toleran reenvíos sin duplicar efectos. Hay tablas y un publicador programado adicionales. Los fallos permanentes requieren revisar las DLQ. La garantía de outbox se aplica a recepciones, no al publicador anterior de pedidos.
