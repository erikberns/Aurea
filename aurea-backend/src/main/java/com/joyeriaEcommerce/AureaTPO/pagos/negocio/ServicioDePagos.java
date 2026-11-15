package com.joyeriaEcommerce.AureaTPO.pagos.negocio;

import com.joyeriaEcommerce.AureaTPO.pagos.datos.EstadoPago;
import com.joyeriaEcommerce.AureaTPO.pagos.datos.Pago;
import com.joyeriaEcommerce.AureaTPO.pagos.datos.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Stateless. Coordina el pago en dos fases con la transacción declarativa de la compra:
 * 1) autoriza (retiene) el importe dentro de la transacción;
 * 2) después del commit lo captura; si la transacción se revierte, anula la retención.
 * Así, una falla a mitad del flujo nunca deja un cobro sin pedido.
 * Los registros de auditoría usan REQUIRES_NEW para sobrevivir al rollback de la compra.
 */
@Service
public class ServicioDePagos implements IPagos {
    private static final Logger log = LoggerFactory.getLogger(ServicioDePagos.class);
    private final IPasarelaPago pasarela;
    private final PagoRepository pagos;
    private final TransactionTemplate nuevaTransaccion;
    private final String moneda;

    public ServicioDePagos(IPasarelaPago pasarela, PagoRepository pagos, PlatformTransactionManager manager,
                           @Value("${app.pagos.moneda:ars}") String moneda) {
        this.pasarela = pasarela;
        this.pagos = pagos;
        this.nuevaTransaccion = new TransactionTemplate(manager);
        this.nuevaTransaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.moneda = moneda.toLowerCase(Locale.ROOT);
    }

    @Override
    public PagoDTO autorizar(Long ordenId, double monto, String metodo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("El pago debe autorizarse dentro de la transacción de compra");
        }
        if (ordenId == null) throw new IllegalArgumentException("La orden es obligatoria");
        MetodoPagoPrueba medio = MetodoPagoPrueba.desde(metodo);
        long centavos = Math.round(monto * 100);
        if (centavos <= 0) throw new IllegalArgumentException("Importe inválido");

        // La clave de idempotencia evita una doble retención si la llamada se reintenta.
        ResultadoPasarelaDTO resultado = pasarela.autorizar(
                "aurea-orden-" + ordenId + "-autorizacion", centavos, moneda, medio.token(), ordenId);
        if (!resultado.aprobado()) {
            registrar(ordenId, resultado.referencia(), medio, centavos, EstadoPago.RECHAZADO, resultado.detalle());
            throw new PagoRechazadoException(resultado.detalle());
        }
        PagoDTO autorizado = registrar(ordenId, resultado.referencia(), medio, centavos, EstadoPago.AUTORIZADO, null);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                if (estado == STATUS_COMMITTED) {
                    capturar(autorizado.id(), autorizado.referenciaExterna());
                } else {
                    anular(autorizado.id(), autorizado.referenciaExterna());
                }
            }
        });
        return autorizado;
    }

    @Override
    public PagoDTO reintentarCaptura(Long pagoId) {
        Pago pago = pagos.findById(pagoId).orElseThrow(() -> new IllegalArgumentException("Pago inexistente"));
        if (pago.estado != EstadoPago.CAPTURA_PENDIENTE) {
            throw new IllegalStateException("Solo se puede reintentar la captura de un pago pendiente de captura");
        }
        capturar(pago.id, pago.referenciaExterna);
        return dto(pagos.findById(pagoId).orElseThrow());
    }

    @Override
    public List<PagoDTO> listar() {
        return pagos.findAllByOrderByCreadoDesc().stream().map(this::dto).toList();
    }

    @Override
    public List<PagoDTO> pagosDeOrden(Long ordenId) {
        return pagos.findByOrdenIdOrderByCreadoDesc(ordenId).stream().map(this::dto).toList();
    }

    @Override
    public ConfiguracionPagoDTO configuracion() {
        var metodos = Arrays.stream(MetodoPagoPrueba.values())
                .map(m -> new MetodoPagoPruebaDTO(m.name(), m.descripcion(), m.aprobacionEsperada()))
                .toList();
        return new ConfiguracionPagoDTO(pasarela.nombre(), moneda, metodos);
    }

    private void capturar(Long pagoId, String referencia) {
        try {
            pasarela.capturar(referencia, "aurea-pago-" + pagoId + "-captura");
            actualizar(pagoId, EstadoPago.CAPTURADO, null);
            log.info("[PAGOS] Pago {} capturado ({})", pagoId, referencia);
        } catch (RuntimeException ex) {
            log.error("[PAGOS] La orden quedó confirmada pero la captura del pago {} falló", pagoId, ex);
            actualizarSinPropagar(pagoId, EstadoPago.CAPTURA_PENDIENTE,
                    "La compra quedó confirmada pero la captura falló; puede reintentarse");
        }
    }

    private void anular(Long pagoId, String referencia) {
        try {
            pasarela.cancelar(referencia, "aurea-pago-" + pagoId + "-anulacion");
            actualizar(pagoId, EstadoPago.ANULADO, "La compra se revirtió; se liberó la retención");
            log.info("[PAGOS] Compra revertida: retención {} anulada", referencia);
        } catch (RuntimeException ex) {
            log.error("[PAGOS] No se pudo anular la retención del pago {}", pagoId, ex);
            actualizarSinPropagar(pagoId, EstadoPago.ANULACION_PENDIENTE,
                    "La compra se revirtió; la pasarela vencerá la retención automáticamente");
        }
    }

    private PagoDTO registrar(Long ordenId, String referencia, MetodoPagoPrueba medio, long centavos,
                              EstadoPago estado, String detalle) {
        return nuevaTransaccion.execute(s -> dto(pagos.save(new Pago(ordenId, pasarela.nombre(), referencia,
                medio.name(), centavos, moneda, estado, detalle))));
    }

    private void actualizar(Long pagoId, EstadoPago estado, String detalle) {
        nuevaTransaccion.executeWithoutResult(s ->
                pagos.findById(pagoId).ifPresent(p -> p.cambiarEstado(estado, detalle)));
    }

    private void actualizarSinPropagar(Long pagoId, EstadoPago estado, String detalle) {
        try {
            actualizar(pagoId, estado, detalle);
        } catch (RuntimeException ex) {
            log.error("[PAGOS] No se pudo registrar el estado {} del pago {}", estado, pagoId, ex);
        }
    }

    private PagoDTO dto(Pago p) {
        return new PagoDTO(p.id, p.ordenId, p.proveedor, p.referenciaExterna, p.metodo, p.montoCentavos,
                p.moneda, p.estado.name(), p.detalle, p.creado, p.actualizado);
    }
}
