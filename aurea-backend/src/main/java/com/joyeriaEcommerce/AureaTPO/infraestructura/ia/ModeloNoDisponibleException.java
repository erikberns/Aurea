package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

/** El proveedor del modelo no respondió o respondió con un error técnico. Nunca incluye la clave de API. */
public class ModeloNoDisponibleException extends RuntimeException {
    public ModeloNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
