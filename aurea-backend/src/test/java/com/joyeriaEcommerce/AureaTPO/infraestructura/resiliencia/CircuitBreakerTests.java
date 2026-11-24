package com.joyeriaEcommerce.AureaTPO.infraestructura.resiliencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.joyeriaEcommerce.AureaTPO.infraestructura.resiliencia.CircuitBreaker.Estado;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CircuitBreakerTests {

    static final class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-11-24T12:00:00Z");
        void avanzar(Duration d) { ahora = ahora.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private final RelojManual reloj = new RelojManual();
    private final CircuitBreaker circuito = new CircuitBreaker("prueba", 3, Duration.ofSeconds(30), reloj);

    private void fallar(int veces) {
        for (int i = 0; i < veces; i++) {
            assertThat(circuito.permitir()).isZero();
            circuito.registrarFallo();
        }
    }

    @Test
    void seAbreAlLlegarAlUmbralYRechazaAlInstanteConLaEsperaRestante() {
        fallar(2);
        assertThat(circuito.estado()).isEqualTo(Estado.CERRADO);
        fallar(1);
        assertThat(circuito.estado()).isEqualTo(Estado.ABIERTO);
        assertThat(circuito.permitir()).isBetween(1L, 31L);
        reloj.avanzar(Duration.ofSeconds(20));
        assertThat(circuito.permitir()).isBetween(1L, 11L);
    }

    @Test
    void unExitoReiniciaLaCuentaDeFallosSeguidos() {
        fallar(2);
        circuito.permitir();
        circuito.registrarExito();
        fallar(2);
        assertThat(circuito.estado()).isEqualTo(Estado.CERRADO);
    }

    @Test
    void semiabiertoDejaPasarUnaSolaPruebaYSeCierraSiResponde() {
        fallar(3);
        reloj.avanzar(Duration.ofSeconds(30));
        assertThat(circuito.permitir()).isZero();
        assertThat(circuito.estado()).isEqualTo(Estado.SEMIABIERTO);
        assertThat(circuito.permitir()).as("una segunda llamada espera el resultado de la prueba").isPositive();
        circuito.registrarExito();
        assertThat(circuito.estado()).isEqualTo(Estado.CERRADO);
        assertThat(circuito.permitir()).isZero();
    }

    @Test
    void siLaPruebaFallaVuelveAAbrirse() {
        fallar(3);
        reloj.avanzar(Duration.ofSeconds(31));
        assertThat(circuito.permitir()).isZero();
        circuito.registrarFallo();
        assertThat(circuito.estado()).isEqualTo(Estado.ABIERTO);
        assertThat(circuito.permitir()).isPositive();
    }

    @Test
    void rechazaUnaConfiguracionInvalida() {
        assertThatThrownBy(() -> new CircuitBreaker("x", 0, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CircuitBreaker("x", 1, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
