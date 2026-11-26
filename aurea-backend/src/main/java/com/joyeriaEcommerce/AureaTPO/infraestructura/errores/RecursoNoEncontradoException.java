package com.joyeriaEcommerce.AureaTPO.infraestructura.errores;

/**
 * El recurso pedido no existe (HTTP 404). Extiende IllegalArgumentException para que el código
 * que ya trataba "no existe" como dato inválido siga funcionando igual.
 */
public class RecursoNoEncontradoException extends IllegalArgumentException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
