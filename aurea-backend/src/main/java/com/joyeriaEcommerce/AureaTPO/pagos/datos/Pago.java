package com.joyeriaEcommerce.AureaTPO.pagos.datos;

import jakarta.persistence.*;
import java.time.Instant;

/** Registro de auditoría de cada intento de pago. Nunca guarda datos de tarjeta. */
@Entity
@Table(name = "pago")
public class Pago {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long ordenId;
    @Column(nullable = false) public String proveedor;
    public String referenciaExterna;
    @Column(nullable = false) public String metodo;
    public long montoCentavos;
    @Column(nullable = false, length = 3) public String moneda;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public EstadoPago estado;
    @Column(length = 500) public String detalle;
    public Instant creado;
    public Instant actualizado;
    @Version public Long version;

    protected Pago() {}

    public Pago(Long ordenId, String proveedor, String referenciaExterna, String metodo,
                long montoCentavos, String moneda, EstadoPago estado, String detalle) {
        this.ordenId = ordenId; this.proveedor = proveedor; this.referenciaExterna = referenciaExterna;
        this.metodo = metodo; this.montoCentavos = montoCentavos; this.moneda = moneda;
        this.estado = estado; this.detalle = detalle;
        this.creado = Instant.now(); this.actualizado = this.creado;
    }

    public void cambiarEstado(EstadoPago nuevo, String detalle) {
        this.estado = nuevo; this.detalle = detalle; this.actualizado = Instant.now();
    }
}
