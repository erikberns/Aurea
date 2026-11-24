package ar.proveedor;

import jakarta.persistence.*;

@Entity
public class Solicitud {
    @Id public String id;
    public String codigo;
    public int cantidad;
    public String estado;
    protected Solicitud() {}
    public Solicitud(String id, String codigo, int cantidad, String estado) {
        this.id = id; this.codigo = codigo; this.cantidad = cantidad; this.estado = estado;
    }
}
