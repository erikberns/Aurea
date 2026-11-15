package com.joyeriaEcommerce.AureaTPO.notificaciones.datos;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "notificacion_administrativa")
public class Notificacion {
    @Id public String id;
    public String mensaje;
    public Instant creada;
    protected Notificacion() {}
    public Notificacion(String id, String mensaje, Instant creada) { this.id = id; this.mensaje = mensaje; this.creada = creada; }
}
