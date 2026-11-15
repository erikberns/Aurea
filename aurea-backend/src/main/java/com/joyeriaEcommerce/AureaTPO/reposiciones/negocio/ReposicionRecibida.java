package com.joyeriaEcommerce.AureaTPO.reposiciones.negocio;
import java.time.Instant;
import java.util.UUID;
public record ReposicionRecibida(int version, String eventId, String reposicionId, Long productoId, int cantidad, Instant ocurrida) {
    public ReposicionRecibida {
        if (version != 1 || eventId == null || !eventId.equals(reposicionId) || productoId == null || productoId <= 0
                || cantidad <= 0 || cantidad > 10000 || ocurrida == null)
            throw new IllegalArgumentException("Evento de reposición inválido");
        UUID.fromString(eventId);
    }
}
