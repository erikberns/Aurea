package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

/** El usuario superó el límite de solicitudes por minuto al modelo (HTTP 429). */
public class LimiteIAExcedidoException extends RuntimeException {

    private final long segundosParaReintentar;

    public LimiteIAExcedidoException(long segundosParaReintentar) {
        super("Hiciste demasiadas solicitudes a la IA en poco tiempo. Esperá " + segundosParaReintentar
                + " segundos y volvé a intentar.");
        this.segundosParaReintentar = segundosParaReintentar;
    }

    public long getSegundosParaReintentar() {
        return segundosParaReintentar;
    }
}
