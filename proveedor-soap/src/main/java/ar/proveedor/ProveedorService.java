package ar.proveedor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import java.util.UUID;

@Service
public class ProveedorService implements ApplicationRunner {
    private final ArticuloRepository articulos;
    private final SolicitudRepository solicitudes;
    public ProveedorService(ArticuloRepository articulos, SolicitudRepository solicitudes) {
        this.articulos = articulos; this.solicitudes = solicitudes;
    }
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String codigo : new String[]{"ANILLO-PLATA", "COLLAR-ORO", "PULSERA-PLATA"}) {
            if (!articulos.existsById(codigo)) articulos.save(new Articulo(codigo, 1000));
        }
    }
    @Transactional(readOnly = true)
    public ResultadoProveedor disponibilidad(String codigo) {
        Articulo a = articulos.findById(codigo).orElseThrow(() -> new IllegalArgumentException("Producto desconocido"));
        return new ResultadoProveedor("", codigo, 0, "DISPONIBLE", a.disponibles);
    }
    @Transactional
    public ResultadoProveedor solicitar(String id, String codigo, int cantidad) {
        UUID.fromString(id);
        if (cantidad <= 0 || cantidad > 10000) throw new IllegalArgumentException("Cantidad fuera de rango");
        // Serializa las solicitudes del mismo artículo. La PK de Solicitud protege también el id global.
        Articulo articulo = articulos.bloquear(codigo).orElseThrow(() -> new IllegalArgumentException("Producto desconocido"));
        var previa = solicitudes.findById(id);
        if (previa.isPresent()) {
            Solicitud s = previa.get();
            if (!s.codigo.equals(codigo) || s.cantidad != cantidad)
                throw new IllegalArgumentException("Identificador reutilizado con otros datos");
            return resultado(s);
        }
        String estado = articulo.disponibles >= cantidad ? "ACEPTADA" : "RECHAZADA";
        if (estado.equals("ACEPTADA")) articulo.disponibles -= cantidad;
        return resultado(solicitudes.saveAndFlush(new Solicitud(id, codigo, cantidad, estado)));
    }
    @Transactional(readOnly = true)
    public ResultadoProveedor consultar(String id) {
        UUID.fromString(id);
        return solicitudes.findById(id).map(this::resultado)
                .orElseGet(() -> new ResultadoProveedor(id, "", 0, "NO_ENCONTRADA", 0));
    }
    private ResultadoProveedor resultado(Solicitud s) {
        return new ResultadoProveedor(s.id, s.codigo, s.cantidad, s.estado, 0);
    }
}
