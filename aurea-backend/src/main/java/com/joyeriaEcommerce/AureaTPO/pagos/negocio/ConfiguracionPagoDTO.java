package com.joyeriaEcommerce.AureaTPO.pagos.negocio;

import java.util.List;

public record ConfiguracionPagoDTO(String proveedor, String moneda, List<MetodoPagoPruebaDTO> metodos) {}
