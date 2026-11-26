package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AIGatewayServiceTests {

    private final TrazaIARepository trazas = mock(TrazaIARepository.class);

    private static ConfiguracionIA config(boolean habilitada, int maxPorMinuto) {
        return new ConfiguracionIA(habilitada, Duration.ofMillis(200), maxPorMinuto, 3, Duration.ofSeconds(30),
                0.30, 2.50);
    }

    private static PedidoIA pedido(String usuario) {
        return new PedidoIA("prueba", "prueba.v1", usuario, new SolicitudModelo("instrucciones", "entrada", 100, 0.2));
    }

    @Test
    void unaRespuestaValidaRegistraLaTrazaConTokensYCosto() {
        var gateway = new AIGatewayService(s -> new RespuestaModelo("{\"ok\":true}", 1000, 200), trazas, config(true, 10));

        ResultadoIA<String> resultado = gateway.completar(pedido("admin"), Optional::of);

        assertThat(resultado.exito()).isTrue();
        assertThat(resultado.valor()).isEqualTo("{\"ok\":true}");
        ArgumentCaptor<TrazaIA> traza = ArgumentCaptor.forClass(TrazaIA.class);
        verify(trazas).save(traza.capture());
        assertThat(traza.getValue().resultado).isEqualTo("OK");
        assertThat(traza.getValue().versionPrompt).isEqualTo("prueba.v1");
        assertThat(traza.getValue().tokensEntrada).isEqualTo(1000);
        assertThat(traza.getValue().tokensSalida).isEqualTo(200);
        assertThat(traza.getValue().costoEstimadoUsd).isCloseTo(0.0008, within(1e-9));
    }

    @Test
    void conLaIAApagadaNoSeLlamaAlModelo() {
        AtomicInteger llamadas = new AtomicInteger();
        var gateway = new AIGatewayService(s -> {
            llamadas.incrementAndGet();
            return new RespuestaModelo("x", 1, 1);
        }, trazas, config(false, 10));

        ResultadoIA<String> resultado = gateway.completar(pedido("admin"), Optional::of);

        assertThat(resultado.resultado()).isEqualTo(ResultadoLlamadaIA.APAGADA);
        assertThat(resultado.aviso()).contains("apagada");
        assertThat(llamadas.get()).isZero();
    }

    @Test
    void siElModeloSuperaElTimeoutSeCortaYSeInforma() {
        var gateway = new AIGatewayService(s -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new RespuestaModelo("tarde", 1, 1);
        }, trazas, config(true, 10));

        long inicio = System.nanoTime();
        ResultadoIA<String> resultado = gateway.completar(pedido("admin"), Optional::of);

        assertThat(resultado.resultado()).isEqualTo(ResultadoLlamadaIA.TIMEOUT);
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofMillis(1500));
    }

    @Test
    void unaRespuestaQueNoPasaLaValidacionNoSeEntrega() {
        var gateway = new AIGatewayService(s -> new RespuestaModelo("texto libre", 10, 10), trazas, config(true, 10));

        ResultadoIA<String> resultado = gateway.completar(pedido("admin"), texto -> Optional.empty());

        assertThat(resultado.resultado()).isEqualTo(ResultadoLlamadaIA.RESPUESTA_INVALIDA);
        assertThat(resultado.valor()).isNull();
        assertThat(resultado.exito()).isFalse();
    }

    @Test
    void tresFallasSeguidasAbrenElCircuitoYNoSeVuelveALlamarAlModelo() {
        AtomicInteger llamadas = new AtomicInteger();
        var gateway = new AIGatewayService(s -> {
            llamadas.incrementAndGet();
            throw new ModeloNoDisponibleException("caído", null);
        }, trazas, config(true, 10));

        for (int i = 0; i < 3; i++) {
            assertThat(gateway.completar(pedido("admin"), Optional::of).resultado())
                    .isEqualTo(ResultadoLlamadaIA.ERROR_MODELO);
        }
        ResultadoIA<String> cuarto = gateway.completar(pedido("admin"), Optional::of);

        assertThat(cuarto.resultado()).isEqualTo(ResultadoLlamadaIA.CIRCUITO_ABIERTO);
        assertThat(cuarto.aviso()).contains("segundos");
        assertThat(llamadas.get()).isEqualTo(3);
        assertThat(gateway.estado().circuito()).isEqualTo("ABIERTO");
    }

    @Test
    void limitaLasSolicitudesPorUsuarioSinAfectarAOtros() {
        var gateway = new AIGatewayService(s -> new RespuestaModelo("x", 1, 1), trazas, config(true, 2));

        gateway.completar(pedido("ana"), Optional::of);
        gateway.completar(pedido("ana"), Optional::of);

        assertThatThrownBy(() -> gateway.completar(pedido("ana"), Optional::of))
                .isInstanceOfSatisfying(LimiteIAExcedidoException.class,
                        ex -> assertThat(ex.getSegundosParaReintentar()).isBetween(1L, 60L));
        assertThat(gateway.completar(pedido("beto"), Optional::of).exito()).isTrue();
    }
}
