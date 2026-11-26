package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Una llamada al modelo. Guarda lo necesario para operar y auditar (versión del prompt, modelo,
 * tokens, costo, latencia y resultado) y deliberadamente NO guarda el texto enviado ni el recibido
 * ni quién lo pidió (política de datos, Ley 25.326).
 */
@Entity
@Table(name = "ia_traza")
public class TrazaIA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Instant creada;
    @Column(length = 60) public String funcionalidad;
    @Column(length = 60) public String versionPrompt;
    @Column(length = 40) public String proveedor;
    @Column(length = 80) public String modelo;
    @Column(length = 30) public String resultado;
    public long latenciaMs;
    public int tokensEntrada;
    public int tokensSalida;
    public double costoEstimadoUsd;

    protected TrazaIA() {
    }

    public TrazaIA(String funcionalidad, String versionPrompt, String proveedor, String modelo, String resultado,
                   long latenciaMs, int tokensEntrada, int tokensSalida, double costoEstimadoUsd) {
        this.creada = Instant.now();
        this.funcionalidad = funcionalidad;
        this.versionPrompt = versionPrompt;
        this.proveedor = proveedor;
        this.modelo = modelo;
        this.resultado = resultado;
        this.latenciaMs = latenciaMs;
        this.tokensEntrada = tokensEntrada;
        this.tokensSalida = tokensSalida;
        this.costoEstimadoUsd = costoEstimadoUsd;
    }
}
