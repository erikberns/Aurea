package com.joyeriaEcommerce.AureaTPO.pagos.negocio;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pasarela local con el mismo contrato que Stripe. Se usa por defecto (PAGOS_PROVEEDOR=simulado)
 * para pruebas automatizadas y demostraciones sin conexión. Respeta la idempotencia por clave.
 */
@Component
@ConditionalOnProperty(name = "app.pagos.proveedor", havingValue = "simulado", matchIfMissing = true)
public class PasarelaSimuladaAdapter implements IPasarelaPago {
    private final Map<String, ResultadoPasarelaDTO> porClave = new ConcurrentHashMap<>();
    private final List<String> operaciones = new CopyOnWriteArrayList<>();

    @Override
    public String nombre() {
        return "simulado";
    }

    @Override
    public ResultadoPasarelaDTO autorizar(String claveIdempotencia, long montoCentavos, String moneda,
                                          String metodoPago, Long ordenId) {
        return porClave.computeIfAbsent(claveIdempotencia, clave -> {
            operaciones.add("autorizar:" + ordenId);
            String referencia = "sim_pi_" + UUID.randomUUID().toString().replace("-", "");
            if (metodoPago.contains("InsufficientFunds")) {
                return new ResultadoPasarelaDTO(false, referencia, "card_declined", "Fondos insuficientes en la tarjeta.");
            }
            if (metodoPago.contains("Declined")) {
                return new ResultadoPasarelaDTO(false, referencia, "card_declined", "La tarjeta fue rechazada por el emisor.");
            }
            return new ResultadoPasarelaDTO(true, referencia, "requires_capture", null);
        });
    }

    @Override
    public void capturar(String referencia, String claveIdempotencia) {
        operaciones.add("capturar:" + referencia);
    }

    @Override
    public void cancelar(String referencia, String claveIdempotencia) {
        operaciones.add("cancelar:" + referencia);
    }

    /** Historial de llamadas, útil para verificar el comportamiento en pruebas. */
    public List<String> operaciones() {
        return List.copyOf(operaciones);
    }
}
