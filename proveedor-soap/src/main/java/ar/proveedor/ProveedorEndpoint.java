package ar.proveedor;

import jakarta.jws.*;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.ws.soap.SOAPFaultException;
import javax.xml.namespace.QName;
import java.util.function.Supplier;

@WebService(serviceName = "ProveedorService", portName = "ProveedorPort", targetNamespace = "urn:aurea:proveedor:v1")
public class ProveedorEndpoint {
    private final ProveedorService service;
    public ProveedorEndpoint(ProveedorService service) { this.service = service; }
    @WebMethod @WebResult(name = "resultado")
    public ResultadoProveedor consultarDisponibilidad(@WebParam(name = "codigoProducto") String codigo) {
        return ejecutar(() -> service.disponibilidad(codigo));
    }
    @WebMethod @WebResult(name = "resultado")
    public ResultadoProveedor solicitarReposicion(@WebParam(name = "idSolicitud") String id,
            @WebParam(name = "codigoProducto") String codigo, @WebParam(name = "cantidad") int cantidad) {
        return ejecutar(() -> service.solicitar(id, codigo, cantidad));
    }
    @WebMethod @WebResult(name = "resultado")
    public ResultadoProveedor consultarEstado(@WebParam(name = "idSolicitud") String id) {
        return ejecutar(() -> service.consultar(id));
    }
    private ResultadoProveedor ejecutar(Supplier<ResultadoProveedor> operacion) {
        try { return operacion.get(); }
        catch (RuntimeException ex) {
            try {
                String mensaje = ex instanceof IllegalArgumentException ? ex.getMessage() : "No se pudo procesar la solicitud";
                throw new SOAPFaultException(SOAPFactory.newInstance().createFault(mensaje,
                        new QName("http://schemas.xmlsoap.org/soap/envelope/", ex instanceof IllegalArgumentException ? "Client" : "Server")));
            } catch (jakarta.xml.soap.SOAPException soap) { throw new IllegalStateException("Error SOAP", soap); }
        }
    }
}
