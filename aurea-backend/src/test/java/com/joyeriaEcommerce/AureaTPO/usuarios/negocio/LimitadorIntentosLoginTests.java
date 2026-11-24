package com.joyeriaEcommerce.AureaTPO.usuarios.negocio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.joyeriaEcommerce.AureaTPO.usuarios.negocio.UsuarioExceptions.DemasiadosIntentosException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LimitadorIntentosLoginTests {

    /** Reloj manual para avanzar el tiempo sin esperar. */
    static final class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-11-24T12:00:00Z");
        void avanzar(Duration d) { ahora = ahora.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private final RelojManual reloj = new RelojManual();
    private final LimitadorIntentosLogin limitador = new LimitadorIntentosLogin(3, 5, Duration.ofMinutes(5), reloj);

    @Test
    void bloqueaElEmailAlSuperarLosFallosYLiberaAlVencerLaVentana() {
        for (int i = 0; i < 3; i++) {
            limitador.verificar("ana@example.com", "10.0.0.1");
            limitador.registrarFallo("ana@example.com", "10.0.0.1");
        }
        assertThatThrownBy(() -> limitador.verificar("ANA@example.com ", "10.0.0.2"))
                .isInstanceOfSatisfying(DemasiadosIntentosException.class,
                        ex -> assertThat(ex.getSegundosParaReintentar()).isBetween(1L, 301L));

        reloj.avanzar(Duration.ofMinutes(5));
        assertThatCode(() -> limitador.verificar("ana@example.com", "10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    void bloqueaElOrigenAunqueSeRoteElEmail() {
        for (int i = 0; i < 5; i++) {
            limitador.registrarFallo("victima" + i + "@example.com", "10.0.0.9");
        }
        assertThatThrownBy(() -> limitador.verificar("otra@example.com", "10.0.0.9"))
                .isInstanceOf(DemasiadosIntentosException.class);
        assertThatCode(() -> limitador.verificar("otra@example.com", "10.0.0.10")).doesNotThrowAnyException();
    }

    @Test
    void unLoginCorrectoLimpiaLosFallosDelEmailPeroNoLosDelOrigen() {
        limitador.registrarFallo("ana@example.com", "10.0.0.3");
        limitador.registrarFallo("ana@example.com", "10.0.0.3");
        limitador.registrarExito("ana@example.com");
        limitador.registrarFallo("ana@example.com", "10.0.0.3");
        limitador.registrarFallo("ana@example.com", "10.0.0.3");
        assertThatCode(() -> limitador.verificar("ana@example.com", "10.0.0.4")).doesNotThrowAnyException();

        limitador.registrarFallo("ana@example.com", "10.0.0.3");
        assertThatThrownBy(() -> limitador.verificar("otra@example.com", "10.0.0.3"))
                .as("el origen acumula sus 5 fallos aunque hubo un login correcto")
                .isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void rechazaUnaConfiguracionInvalida() {
        assertThatThrownBy(() -> new LimitadorIntentosLogin(0, 5, Duration.ofMinutes(1), reloj))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LimitadorIntentosLogin(3, 5, Duration.ZERO, reloj))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
