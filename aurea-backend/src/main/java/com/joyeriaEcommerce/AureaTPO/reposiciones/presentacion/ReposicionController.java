package com.joyeriaEcommerce.AureaTPO.reposiciones.presentacion;
import com.joyeriaEcommerce.AureaTPO.reposiciones.negocio.IReposiciones;
import com.joyeriaEcommerce.AureaTPO.reposiciones.negocio.ReposicionDTO;
import com.joyeriaEcommerce.AureaTPO.reposiciones.negocio.ResultadoProveedorDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/reposiciones")
@PreAuthorize("hasRole('ADMIN')")
public class ReposicionController {
    private final IReposiciones reposiciones;
    public ReposicionController(IReposiciones reposiciones) { this.reposiciones = reposiciones; }
    @GetMapping public List<ReposicionDTO> listar() { return reposiciones.listar(); }
    @GetMapping("/disponibilidad")
    public ResultadoProveedorDTO disponibilidad(@RequestParam String codigo) { return reposiciones.disponibilidad(codigo); }
    @PostMapping
    public ResponseEntity<ReposicionDTO> solicitar(@Valid @RequestBody SolicitudRequest r) {
        var resultado = reposiciones.solicitar(r.idSolicitud(), r.productoId(), r.codigoProveedor(), r.cantidad());
        return ResponseEntity.accepted().body(resultado);
    }
    @PostMapping("/{id}/sincronizar") public ReposicionDTO sincronizar(@PathVariable String id) { return reposiciones.sincronizar(id); }
    @PostMapping("/{id}/recibir") public ReposicionDTO recibir(@PathVariable String id) { return reposiciones.recibir(id); }
    public record SolicitudRequest(@NotBlank String idSolicitud, @NotNull @Positive Long productoId,
            @NotBlank @Pattern(regexp = "[A-Z0-9-]{1,64}") String codigoProveedor, @Min(1) @Max(10000) int cantidad) {}
}
