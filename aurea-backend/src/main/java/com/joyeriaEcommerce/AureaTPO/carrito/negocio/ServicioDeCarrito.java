package com.joyeriaEcommerce.AureaTPO.carrito.negocio;

import com.joyeriaEcommerce.AureaTPO.carrito.datos.ICarritoRepository;

import com.joyeriaEcommerce.AureaTPO.productos.negocio.ProductDTO;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.IProductos;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.ICheckout;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.OrderDTO;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.ICalculoEnvio;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Estado conversacional: Spring crea una instancia por sesión HTTP.
 * Conserva identificadores y cantidades entre solicitudes, sin guardar entidades JPA.
 * Agregar un producto no reserva stock; la compra vuelve a validar las existencias.
 */
@Service
@SessionScope
public class ServicioDeCarrito implements ICarrito {

    private static final Logger log = LoggerFactory.getLogger(ServicioDeCarrito.class);
    private final String instancia = UUID.randomUUID().toString();
    private final ICarritoRepository seleccion;
    private final IProductos productos;
    private final ICheckout checkout;
    private final ICalculoEnvio envio;
    private boolean destruido;

    public ServicioDeCarrito(IProductos productos, ICheckout checkout, ICalculoEnvio envio,
                             ICarritoRepository seleccion) {
        this.productos = productos;
        this.checkout = checkout;
        this.envio = envio;
        this.seleccion = seleccion;
    }

    @PostConstruct
    public void iniciar() {
        log.info("[CICLO DE VIDA] Carrito {} creado", instancia);
    }

    @PreDestroy
    public synchronized void destruir() {
        seleccion.vaciar();
        destruido = true;
        log.info("[CICLO DE VIDA] Carrito {} destruido", instancia);
    }

    private void verificarActivo() {
        if (destruido) {
            throw new IllegalStateException("La sesión del carrito terminó");
        }
    }

    @Override
    public synchronized CarritoDTO consultar() {
        verificarActivo();
        List<CarritoDTO.Item> items = new ArrayList<>();
        double subtotal = 0;
        int cantidadTotal = 0;
        for (var entry : seleccion.consultar().entrySet()) {
            ProductDTO producto = productos.getProductById(entry.getKey());
            double precio = producto.price();
            items.add(new CarritoDTO.Item(producto.id(), producto.name(), producto.imageUrl(),
                    precio, entry.getValue(), "Única"));
            subtotal += precio * entry.getValue();
            cantidadTotal += entry.getValue();
        }
        double costo = items.isEmpty() ? 0 : envio.calcular(subtotal);
        return new CarritoDTO(List.copyOf(items), subtotal, costo, subtotal + costo, cantidadTotal);
    }

    @Override
    public synchronized CarritoDTO agregar(Long productoId, int cantidad) {
        verificarActivo();
        if (productoId == null || cantidad <= 0) {
            throw new IllegalArgumentException("Producto y cantidad inválidos");
        }
        ProductDTO producto = productos.getProductById(productoId);
        long nuevaCantidad = (long) seleccion.cantidadDe(productoId) + cantidad;
        if (!Boolean.TRUE.equals(producto.active())) {
            throw new IllegalStateException("El producto no está disponible");
        }
        if (producto.stock() == null || nuevaCantidad > producto.stock()) {
            throw new IllegalStateException("Stock insuficiente para " + producto.name());
        }
        seleccion.guardar(productoId, (int) nuevaCantidad);
        return consultar();
    }

    @Override
    public synchronized CarritoDTO quitar(Long productoId) {
        verificarActivo();
        seleccion.quitar(productoId);
        return consultar();
    }

    @Override
    public synchronized CarritoDTO vaciar() {
        verificarActivo();
        seleccion.vaciar();
        return consultar();
    }

    @Override
    public synchronized OrderDTO comprar(String usuario, String direccion) {
        verificarActivo();
        // El proxy de la fachada retorna después del commit. Un fallo conserva la selección.
        OrderDTO orden = checkout.purchase(usuario, direccion, seleccion.consultar());
        seleccion.vaciar();
        return orden;
    }
}
