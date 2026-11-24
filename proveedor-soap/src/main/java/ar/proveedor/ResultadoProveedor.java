package ar.proveedor;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResultadoProveedor", propOrder = {"idSolicitud", "codigoProducto", "cantidad", "estado", "disponibles"})
public class ResultadoProveedor {
    public String idSolicitud;
    public String codigoProducto;
    public int cantidad;
    public String estado;
    public int disponibles;
    public ResultadoProveedor() {}
    public ResultadoProveedor(String id, String codigo, int cantidad, String estado, int disponibles) {
        this.idSolicitud = id; this.codigoProducto = codigo; this.cantidad = cantidad;
        this.estado = estado; this.disponibles = disponibles;
    }
}
