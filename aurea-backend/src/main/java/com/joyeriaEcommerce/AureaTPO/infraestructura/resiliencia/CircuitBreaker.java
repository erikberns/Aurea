package com.joyeriaEcommerce.AureaTPO.infraestructura.resiliencia;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Circuit breaker para llamadas a sistemas externos (Stripe y el proveedor SOAP).
 *
 * <p>Cerrado: deja pasar las llamadas y cuenta los fallos seguidos (sin respuesta o error 5xx).
 * Al llegar al umbral se abre: durante la espera rechaza las llamadas al instante, sin esperar
 * los 5 s del timeout, y quien llama aplica su fallback. Al vencer la espera pasa a semiabierto
 * y deja pasar una sola llamada de prueba: si responde, se cierra; si falla, vuelve a abrirse.</p>
 *
 * <p>Cada llamada permitida debe terminar en {@link #registrarExito()} o {@link #registrarFallo()}.
 * Una respuesta de negocio (tarjeta rechazada, SOAP Fault) cuenta como éxito: el sistema respondió.</p>
 */
public final class CircuitBreaker {

    public enum Estado { CERRADO, ABIERTO, SEMIABIERTO }

    private final String nombre;
    private final int umbralFallos;
    private final Duration espera;
    private final Clock reloj;

    private Estado estado = Estado.CERRADO;
    private int fallosSeguidos;
    private Instant abiertoHasta = Instant.MIN;
    private boolean pruebaEnCurso;

    public CircuitBreaker(String nombre, int umbralFallos, Duration espera) {
        this(nombre, umbralFallos, espera, Clock.systemUTC());
    }

    public CircuitBreaker(String nombre, int umbralFallos, Duration espera, Clock reloj) {
        if (umbralFallos < 1 || espera.isNegative() || espera.isZero()) {
            throw new IllegalArgumentException("Configuración inválida del circuit breaker " + nombre);
        }
        this.nombre = nombre;
        this.umbralFallos = umbralFallos;
        this.espera = espera;
        this.reloj = reloj;
    }

    /**
     * Pide permiso para llamar al sistema externo.
     *
     * @return 0 si se puede llamar; si no, los segundos que faltan para volver a intentar.
     */
    public synchronized long permitir() {
        Instant ahora = reloj.instant();
        if (estado == Estado.ABIERTO) {
            if (ahora.isBefore(abiertoHasta)) {
                return Math.max(1, (Duration.between(ahora, abiertoHasta).toMillis() + 999) / 1000);
            }
            estado = Estado.SEMIABIERTO;
        }
        if (estado == Estado.SEMIABIERTO) {
            if (pruebaEnCurso) {
                return 1;
            }
            pruebaEnCurso = true;
        }
        return 0;
    }

    public synchronized void registrarExito() {
        estado = Estado.CERRADO;
        fallosSeguidos = 0;
        pruebaEnCurso = false;
    }

    public synchronized void registrarFallo() {
        pruebaEnCurso = false;
        if (estado == Estado.SEMIABIERTO) {
            abrir();
            return;
        }
        fallosSeguidos++;
        if (fallosSeguidos >= umbralFallos) {
            abrir();
        }
    }

    public synchronized Estado estado() {
        return estado;
    }

    public String nombre() {
        return nombre;
    }

    private void abrir() {
        estado = Estado.ABIERTO;
        abiertoHasta = reloj.instant().plus(espera);
        fallosSeguidos = 0;
    }
}
