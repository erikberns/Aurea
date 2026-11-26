package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

import com.joyeriaEcommerce.AureaTPO.infraestructura.resiliencia.CircuitBreaker;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementación del {@link IAGateway}. Orden de las protecciones en cada llamada:
 * feature flag → límite por usuario → circuit breaker → llamada con timeout → validación de la respuesta.
 */
@Component
public class AIGatewayService implements IAGateway {

    private static final Logger log = LoggerFactory.getLogger(AIGatewayService.class);
    private static final Duration VENTANA_LIMITE = Duration.ofMinutes(1);

    private final ProveedorModeloIA proveedor;
    private final TrazaIARepository trazas;
    private final ConfiguracionIA config;
    private final Clock reloj;
    private final CircuitBreaker circuito;
    private final ExecutorService ejecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, Deque<Instant>> solicitudesPorUsuario = new ConcurrentHashMap<>();

    @Autowired
    public AIGatewayService(ProveedorModeloIA proveedor, TrazaIARepository trazas, ConfiguracionIA config) {
        this(proveedor, trazas, config, Clock.systemUTC());
    }

    public AIGatewayService(ProveedorModeloIA proveedor, TrazaIARepository trazas, ConfiguracionIA config, Clock reloj) {
        this.proveedor = proveedor;
        this.trazas = trazas;
        this.config = config;
        this.reloj = reloj;
        this.circuito = new CircuitBreaker("ia-" + proveedor.nombre(), config.fallosParaAbrir(),
                config.esperaCircuito(), reloj);
    }

    @Override
    public <T> ResultadoIA<T> completar(PedidoIA pedido, Function<String, Optional<T>> interpretar) {
        long inicio = System.nanoTime();
        if (!config.habilitada()) {
            return cerrar(pedido, ResultadoLlamadaIA.APAGADA, inicio, null, null,
                    "La asistencia con IA está apagada. Se armó una plantilla para que completes a mano.");
        }
        verificarLimite(pedido.usuario());

        long espera = circuito.permitir();
        if (espera > 0) {
            return cerrar(pedido, ResultadoLlamadaIA.CIRCUITO_ABIERTO, inicio, null, null,
                    "El servicio de IA no está respondiendo. Se armó una plantilla; podés volver a intentar en "
                            + espera + " segundos.");
        }

        RespuestaModelo respuesta;
        Future<RespuestaModelo> llamada = ejecutor.submit(() -> proveedor.generar(pedido.solicitud()));
        try {
            respuesta = llamada.get(config.timeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            llamada.cancel(true);
            circuito.registrarFallo();
            return cerrar(pedido, ResultadoLlamadaIA.TIMEOUT, inicio, null, null,
                    "La IA no respondió a tiempo. Se armó una plantilla para que completes a mano.");
        } catch (ExecutionException ex) {
            circuito.registrarFallo();
            Throwable causa = ex.getCause() == null ? ex : ex.getCause();
            log.warn("El modelo {} no respondió correctamente: {}", proveedor.nombre(), causa.getMessage());
            return cerrar(pedido, ResultadoLlamadaIA.ERROR_MODELO, inicio, null, null,
                    "La IA no está disponible en este momento. Se armó una plantilla para que completes a mano.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            circuito.registrarFallo();
            return cerrar(pedido, ResultadoLlamadaIA.ERROR_MODELO, inicio, null, null,
                    "La consulta a la IA se interrumpió. Se armó una plantilla para que completes a mano.");
        }
        // El modelo respondió: para el circuito es un éxito aunque el contenido después no sirva.
        circuito.registrarExito();

        Optional<T> valor;
        try {
            valor = respuesta == null ? Optional.empty() : interpretar.apply(respuesta.texto());
        } catch (RuntimeException ex) {
            valor = Optional.empty();
        }
        if (valor == null || valor.isEmpty()) {
            return cerrar(pedido, ResultadoLlamadaIA.RESPUESTA_INVALIDA, inicio, respuesta, null,
                    "La respuesta de la IA no pasó las validaciones de seguridad y calidad. Se armó una plantilla "
                            + "para que completes a mano.");
        }
        return cerrar(pedido, ResultadoLlamadaIA.OK, inicio, respuesta, valor.get(), null);
    }

    @Override
    public EstadoGatewayIA estado() {
        return new EstadoGatewayIA(config.habilitada(), proveedor.nombre(), proveedor.modelo(),
                circuito.estado().name());
    }

    /** Ventana deslizante de un minuto por usuario. */
    private void verificarLimite(String usuario) {
        String clave = usuario == null || usuario.isBlank() ? "anonimo" : usuario;
        Instant ahora = reloj.instant();
        Deque<Instant> marcas = solicitudesPorUsuario.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (marcas) {
            while (!marcas.isEmpty() && !marcas.peekFirst().isAfter(ahora.minus(VENTANA_LIMITE))) {
                marcas.pollFirst();
            }
            if (marcas.size() >= config.maxSolicitudesPorMinuto()) {
                long restantes = Duration.between(ahora, marcas.peekFirst().plus(VENTANA_LIMITE)).toMillis();
                throw new LimiteIAExcedidoException(Math.max(1, (restantes + 999) / 1000));
            }
            marcas.addLast(ahora);
        }
    }

    private <T> ResultadoIA<T> cerrar(PedidoIA pedido, ResultadoLlamadaIA resultado, long inicio,
                                      RespuestaModelo respuesta, T valor, String aviso) {
        long latenciaMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
        int entrada = respuesta == null ? 0 : Math.max(0, respuesta.tokensEntrada());
        int salida = respuesta == null ? 0 : Math.max(0, respuesta.tokensSalida());
        double costo = entrada * config.costoEntradaUsdPorMillon() / 1_000_000d
                + salida * config.costoSalidaUsdPorMillon() / 1_000_000d;
        try {
            trazas.save(new TrazaIA(pedido.funcionalidad(), pedido.versionPrompt(), proveedor.nombre(),
                    proveedor.modelo(), resultado.name(), latenciaMs, entrada, salida, costo));
        } catch (RuntimeException ex) {
            // La traza nunca debe romper la funcionalidad.
            log.warn("No se pudo registrar la traza de IA: {}", ex.getMessage());
        }
        log.info("IA {} [{} {}]: {} en {} ms, tokens {}/{}", pedido.funcionalidad(), proveedor.nombre(),
                pedido.versionPrompt(), resultado, latenciaMs, entrada, salida);
        return new ResultadoIA<>(valor, resultado, aviso);
    }
}
