package com.joyeriaEcommerce.AureaTPO.pagos.negocio;

import java.util.List;

/** Contrato público del componente Pagos. */
public interface IPagos {
    /**
     * Autoriza (retiene) el importe dentro de la transacción de compra en curso.
     * Tras el commit se captura; si la transacción se revierte, se anula la retención.
     * Lanza PagoRechazadoException si la tarjeta es rechazada.
     */
    PagoDTO autorizar(Long ordenId, double monto, String metodo);
    PagoDTO reintentarCaptura(Long pagoId);
    List<PagoDTO> listar();
    List<PagoDTO> pagosDeOrden(Long ordenId);
    ConfiguracionPagoDTO configuracion();
}
