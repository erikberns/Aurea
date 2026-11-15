package com.joyeriaEcommerce.AureaTPO.reposiciones.negocio;
public interface IProveedorGateway {
    ResultadoProveedorDTO disponibilidad(String codigo);
    ResultadoProveedorDTO solicitar(String id, String codigo, int cantidad);
    ResultadoProveedorDTO consultar(String id);
}
