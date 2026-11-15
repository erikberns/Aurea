package com.joyeriaEcommerce.AureaTPO.reposiciones.negocio;
import java.time.Instant;
public record ReposicionDTO(String id, Long productoId, String codigoProveedor, int cantidad, String estado,
        Instant creada, Instant recibida, String detalle, boolean eventoPublicado, boolean inventarioActualizado) {}
