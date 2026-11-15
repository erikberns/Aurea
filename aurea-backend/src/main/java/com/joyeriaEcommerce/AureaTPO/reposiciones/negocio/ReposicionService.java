package com.joyeriaEcommerce.AureaTPO.reposiciones.negocio;
import com.joyeriaEcommerce.AureaTPO.reposiciones.datos.Reposicion;
import com.joyeriaEcommerce.AureaTPO.reposiciones.datos.ReposicionRepository;
import com.joyeriaEcommerce.AureaTPO.reposiciones.datos.EventoPendiente;
import com.joyeriaEcommerce.AureaTPO.reposiciones.datos.EventoPendienteRepository;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.IProductos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;

@Service
public class ReposicionService implements IReposiciones {
    private final ReposicionRepository reposiciones;
    private final EventoPendienteRepository eventos;
    private final IProveedorGateway proveedor;
    private final IProductos productos;
    private final ObjectMapper json;
    private final TransactionTemplate tx;
    public ReposicionService(ReposicionRepository reposiciones, EventoPendienteRepository eventos,
            IProveedorGateway proveedor, IProductos productos, ObjectMapper json, PlatformTransactionManager manager) {
        this.reposiciones = reposiciones; this.eventos = eventos; this.proveedor = proveedor;
        this.productos = productos; this.json = json; tx = new TransactionTemplate(manager);
    }
    public List<ReposicionDTO> listar() { return reposiciones.findAllByOrderByCreadaDesc().stream().map(this::dto).toList(); }
    public ResultadoProveedorDTO disponibilidad(String codigo) { return proveedor.disponibilidad(codigo); }
    public ReposicionDTO solicitar(String id, Long productoId, String codigo, int cantidad) {
        UUID.fromString(id);
        if (cantidad <= 0 || cantidad > 10000 || codigo == null || !codigo.matches("[A-Z0-9-]{1,64}"))
            throw new IllegalArgumentException("Código o cantidad inválidos");
        productos.getProductById(productoId);
        try {
            tx.executeWithoutResult(s -> {
                if (!reposiciones.existsById(id)) reposiciones.saveAndFlush(new Reposicion(id, productoId, codigo, cantidad));
            });
        } catch (DataIntegrityViolationException concurrente) {
            if (!reposiciones.existsById(id)) throw concurrente;
        }
        Reposicion r = buscar(id);
        if (!r.productoId.equals(productoId) || !r.codigoProveedor.equals(codigo) || r.cantidad != cantidad)
            throw new IllegalStateException("Identificador de solicitud reutilizado con otros datos");
        return sincronizar(id);
    }
    public ReposicionDTO sincronizar(String id) {
        Reposicion r = buscar(id);
        if (!r.estado.equals("SOLICITADA")) return dto(r);
        try {
            // No se mantiene una transacción de base de datos abierta durante la llamada remota.
            ResultadoProveedorDTO resultado = proveedor.consultar(id);
            if (resultado.estado().equals("NO_ENCONTRADA")) resultado = proveedor.solicitar(id, r.codigoProveedor, r.cantidad);
            if (!id.equals(resultado.idSolicitud()) || !r.codigoProveedor.equals(resultado.codigoProducto())
                    || r.cantidad != resultado.cantidad()
                    || !Set.of("ACEPTADA", "RECHAZADA").contains(resultado.estado()))
                throw new ProveedorNoDisponibleException("El proveedor respondió datos incompatibles con la solicitud", null);
            ResultadoProveedorDTO respuesta = resultado;
            tx.executeWithoutResult(s -> {
                Reposicion actual = bloquear(id);
                if (actual.estado.equals("SOLICITADA")) { actual.estado = respuesta.estado(); actual.detalle = null; }
            });
        } catch (ProveedorNoDisponibleException ex) {
            tx.executeWithoutResult(s -> {
                Reposicion actual = bloquear(id);
                if (actual.estado.equals("SOLICITADA")) actual.detalle = ex.getMessage();
            });
        }
        return dto(buscar(id));
    }
    public ReposicionDTO recibir(String id) {
        tx.executeWithoutResult(s -> {
            Reposicion r = bloquear(id);
            if (r.estado.equals("RECIBIDA")) return; // Doble clic/reintento: una sola recepción y un solo evento.
            if (!r.estado.equals("ACEPTADA")) throw new IllegalStateException("Solo se puede recibir una reposición aceptada");
            r.estado = "RECIBIDA"; r.recibida = Instant.now();
            var evento = new ReposicionRecibida(1, r.id, r.id, r.productoId, r.cantidad, r.recibida);
            eventos.save(new EventoPendiente(r.id, json.writeValueAsString(evento)));
        });
        return dto(buscar(id));
    }
    private Reposicion buscar(String id) {
        return reposiciones.findById(id).orElseThrow(() -> new IllegalArgumentException("Reposición inexistente"));
    }
    private Reposicion bloquear(String id) {
        return reposiciones.bloquear(id).orElseThrow(() -> new IllegalArgumentException("Reposición inexistente"));
    }
    private ReposicionDTO dto(Reposicion r) {
        boolean publicado = eventos.findById(r.id).map(e -> e.publicado != null).orElse(false);
        return new ReposicionDTO(r.id, r.productoId, r.codigoProveedor, r.cantidad, r.estado, r.creada, r.recibida,
                r.detalle, publicado, productos.reposicionAplicada(r.id));
    }
}
