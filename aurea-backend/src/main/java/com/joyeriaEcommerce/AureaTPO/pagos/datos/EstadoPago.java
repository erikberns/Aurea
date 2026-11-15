package com.joyeriaEcommerce.AureaTPO.pagos.datos;

/** Estados de un intento de pago frente a la pasarela externa. */
public enum EstadoPago {
    /** La pasarela retuvo el importe; todavía no se cobró. */
    AUTORIZADO,
    /** El importe retenido se cobró después del commit de la compra. */
    CAPTURADO,
    /** La pasarela rechazó la tarjeta; la compra se revirtió. */
    RECHAZADO,
    /** La compra falló después de autorizar: se liberó la retención. */
    ANULADO,
    /** La compra se confirmó pero la captura falló; un administrador puede reintentarla. */
    CAPTURA_PENDIENTE,
    /** No se pudo liberar la retención; la pasarela la vence automáticamente. */
    ANULACION_PENDIENTE
}
