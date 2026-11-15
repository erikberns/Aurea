package com.joyeriaEcommerce.AureaTPO.reposiciones.datos;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "reposicion")
public class Reposicion {
    @Id public String id;
    @Column(nullable = false) public Long productoId;
    @Column(nullable = false) public String codigoProveedor;
    public int cantidad;
    @Column(nullable = false) public String estado;
    public Instant creada;
    public Instant recibida;
    @Column(length = 500) public String detalle;
    @Version public Long version;
    protected Reposicion() {}
    public Reposicion(String id, Long productoId, String codigo, int cantidad) {
        this.id = id; this.productoId = productoId; this.codigoProveedor = codigo;
        this.cantidad = cantidad; this.estado = "SOLICITADA"; this.creada = Instant.now();
    }
}
