package com.joyeriaEcommerce.AureaTPO.reposiciones.datos;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "reposicion_outbox")
public class EventoPendiente {
    @Id public String id;
    @Column(nullable = false, length = 4000) public String payload;
    public Instant creado;
    public Instant publicado;
    public int intentos;
    public Instant proximoIntento;
    @Column(length = 500) public String ultimoError;
    protected EventoPendiente() {}
    public EventoPendiente(String id, String payload) {
        this.id = id; this.payload = payload; creado = Instant.now(); proximoIntento = creado;
    }
}
