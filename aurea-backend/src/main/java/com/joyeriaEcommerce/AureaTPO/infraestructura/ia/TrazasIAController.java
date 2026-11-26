package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Trazas de las llamadas al modelo, para el administrador: evidencia de operación y costo. */
@RestController
@RequestMapping("/api/ia")
public class TrazasIAController {

    private final TrazaIARepository trazas;
    private final IAGateway gateway;

    public TrazasIAController(TrazaIARepository trazas, IAGateway gateway) {
        this.trazas = trazas;
        this.gateway = gateway;
    }

    public record TrazaDTO(Instant creada, String funcionalidad, String versionPrompt, String proveedor,
                           String modelo, String resultado, long latenciaMs, int tokensEntrada, int tokensSalida,
                           double costoEstimadoUsd) {}

    public record ResumenTrazasDTO(EstadoGatewayIA estado, int llamadas, Map<String, Long> porResultado,
                                   long tokensEntrada, long tokensSalida, double costoEstimadoUsd,
                                   long latenciaP95Ms, List<TrazaDTO> ultimas) {}

    /** Resumen de las últimas 200 llamadas y el detalle de las 20 más recientes. */
    @GetMapping("/trazas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResumenTrazasDTO trazas() {
        List<TrazaIA> recientes = trazas.findTop200ByOrderByCreadaDesc();
        Map<String, Long> porResultado = new TreeMap<>();
        long entrada = 0;
        long salida = 0;
        double costo = 0;
        for (TrazaIA t : recientes) {
            porResultado.merge(t.resultado, 1L, Long::sum);
            entrada += t.tokensEntrada;
            salida += t.tokensSalida;
            costo += t.costoEstimadoUsd;
        }
        List<Long> latencias = recientes.stream().map(t -> t.latenciaMs).sorted(Comparator.naturalOrder()).toList();
        long p95 = latencias.isEmpty() ? 0 : latencias.get((int) Math.ceil(latencias.size() * 0.95) - 1);
        List<TrazaDTO> ultimas = recientes.stream().limit(20)
                .map(t -> new TrazaDTO(t.creada, t.funcionalidad, t.versionPrompt, t.proveedor, t.modelo,
                        t.resultado, t.latenciaMs, t.tokensEntrada, t.tokensSalida, t.costoEstimadoUsd))
                .toList();
        return new ResumenTrazasDTO(gateway.estado(), recientes.size(), new LinkedHashMap<>(porResultado),
                entrada, salida, costo, p95, ultimas);
    }
}
