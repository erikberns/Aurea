package com.joyeriaEcommerce.AureaTPO.reposiciones;

import com.joyeriaEcommerce.AureaTPO.reposiciones.negocio.ReposicionRecibida;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Contrato del evento aurea.reposiciones v1: un mensaje con cualquier campo inválido se rechaza (va a la DLQ). */
class ReposicionRecibidaTests {
    private static final String ID = UUID.randomUUID().toString();
    private static final Instant AHORA = Instant.now();

    @Test
    void eventoValidoSeAcepta() {
        var evento = new ReposicionRecibida(1, ID, ID, 7L, 3, AHORA);
        assertThat(evento.cantidad()).isEqualTo(3);
    }

    @Test
    void cadaCampoInvalidoSeRechaza() {
        String otro = UUID.randomUUID().toString();
        assertInvalido(() -> new ReposicionRecibida(2, ID, ID, 7L, 3, AHORA));
        assertInvalido(() -> new ReposicionRecibida(1, null, ID, 7L, 3, AHORA));
        assertInvalido(() -> new ReposicionRecibida(1, ID, otro, 7L, 3, AHORA));
        assertInvalido(() -> new ReposicionRecibida(1, ID, ID, null, 3, AHORA));
        assertInvalido(() -> new ReposicionRecibida(1, ID, ID, 0L, 3, AHORA));
        assertInvalido(() -> new ReposicionRecibida(1, ID, ID, 7L, 0, AHORA));
        assertInvalido(() -> new ReposicionRecibida(1, ID, ID, 7L, 10001, AHORA));
        assertInvalido(() -> new ReposicionRecibida(1, ID, ID, 7L, 3, null));
        assertInvalido(() -> new ReposicionRecibida(1, "no-uuid", "no-uuid", 7L, 3, AHORA));
    }

    private static void assertInvalido(org.assertj.core.api.ThrowableAssert.ThrowingCallable creacion) {
        assertThatThrownBy(creacion).isInstanceOf(IllegalArgumentException.class);
    }
}
