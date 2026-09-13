package com.joyeriaEcommerce.AureaTPO.carrito.negocio;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.OrderDTO;
public interface ICarrito {
 CarritoDTO consultar(); CarritoDTO agregar(Long productoId, int cantidad);
 CarritoDTO quitar(Long productoId); CarritoDTO vaciar();
 OrderDTO comprar(String usuario, String direccion);
}
