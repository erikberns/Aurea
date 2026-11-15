package com.joyeriaEcommerce.AureaTPO.reposiciones.negocio;
import java.util.List;
public interface IReposiciones {
    List<ReposicionDTO> listar();
    ResultadoProveedorDTO disponibilidad(String codigo);
    ReposicionDTO solicitar(String id, Long productoId, String codigo, int cantidad);
    ReposicionDTO sincronizar(String id);
    ReposicionDTO recibir(String id);
}
