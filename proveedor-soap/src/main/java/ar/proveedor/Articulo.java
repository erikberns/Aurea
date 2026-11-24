package ar.proveedor;

import jakarta.persistence.*;

@Entity
public class Articulo {
    @Id public String codigo;
    public int disponibles;
    protected Articulo() {}
    public Articulo(String codigo, int disponibles) { this.codigo = codigo; this.disponibles = disponibles; }
}
