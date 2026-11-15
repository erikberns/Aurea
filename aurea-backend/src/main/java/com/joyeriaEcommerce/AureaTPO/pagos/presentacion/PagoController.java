package com.joyeriaEcommerce.AureaTPO.pagos.presentacion;

import com.joyeriaEcommerce.AureaTPO.pagos.negocio.ConfiguracionPagoDTO;
import com.joyeriaEcommerce.AureaTPO.pagos.negocio.IPagos;
import com.joyeriaEcommerce.AureaTPO.pagos.negocio.PagoDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {
    private final IPagos pagos;

    public PagoController(IPagos pagos) {
        this.pagos = pagos;
    }

    /** Pasarela activa y métodos de prueba disponibles para el checkout. */
    @GetMapping("/configuracion")
    @PreAuthorize("isAuthenticated()")
    public ConfiguracionPagoDTO configuracion() {
        return pagos.configuracion();
    }

    /** Auditoría de intentos de pago. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<PagoDTO> listar() {
        return pagos.listar();
    }

    /** Reintenta la captura de una compra confirmada cuyo cobro quedó pendiente. */
    @PostMapping("/{id}/capturar")
    @PreAuthorize("hasRole('ADMIN')")
    public PagoDTO reintentarCaptura(@PathVariable Long id) {
        return pagos.reintentarCaptura(id);
    }
}
