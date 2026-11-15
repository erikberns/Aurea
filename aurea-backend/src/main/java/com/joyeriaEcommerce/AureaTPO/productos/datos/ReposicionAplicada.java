package com.joyeriaEcommerce.AureaTPO.productos.datos;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "inventario_reposicion_aplicada")
public class ReposicionAplicada {
    @Id public String id;
    public Long productoId;
    public int cantidad;
    public Instant aplicada;
    protected ReposicionAplicada() {}
    public ReposicionAplicada(String id, Long productoId, int cantidad) {
        this.id = id; this.productoId = productoId; this.cantidad = cantidad; aplicada = Instant.now();
    }
}
