package com.joyeriaEcommerce.AureaTPO.pagos.negocio;

import java.time.Instant;

public record PagoDTO(Long id, Long ordenId, String proveedor, String referenciaExterna, String metodo,
                      long montoCentavos, String moneda, String estado, String detalle,
                      Instant creado, Instant actualizado) {}
