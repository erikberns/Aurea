package com.joyeriaEcommerce.AureaTPO.ordenes.datos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Outbox de pedidos: el aviso de "pedido confirmado" se guarda en la misma transacción que la
 * confirmación. Si RabbitMQ está caído, el evento queda acá y se publica cuando el broker vuelve.
 */
@Entity
@Table(name = "pedido_outbox")
public class EventoPedidoPendiente {
    @Id public String id;
    @Column(nullable = false, length = 4000) public String payload;
    public Instant creado;
    public Instant publicado;
    public int intentos;
    public Instant proximoIntento;
    @Column(length = 500) public String ultimoError;

    protected EventoPedidoPendiente() {}

    public EventoPedidoPendiente(String id, String payload) {
        this.id = id;
        this.payload = payload;
        creado = Instant.now();
        proximoIntento = creado;
    }
}
