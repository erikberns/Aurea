package ar.proveedor;

import jakarta.xml.ws.Endpoint;
import java.util.concurrent.Executors;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@SpringBootApplication
public class ProveedorApplication {
    public static void main(String[] args) { SpringApplication.run(ProveedorApplication.class, args); }

    /**
     * Publica el servicio SOAP con hilos propios que usan el class loader de la aplicación.
     * Dentro del .jar de Spring Boot, las librerías (SAAJ y JAXB de Metro) solo las encuentra ese class loader:
     * con los hilos por defecto del servidor HTTP del JDK no se podían construir los SOAP Fault y el proveedor
     * respondía HTTP 500 vacío ante cualquier error de negocio (por ejemplo, un código de producto desconocido).
     */
    @Bean(destroyMethod = "stop")
    @ConditionalOnProperty(name = "proveedor.soap.enabled", havingValue = "true", matchIfMissing = true)
    Endpoint endpoint(ProveedorService service, @Value("${proveedor.endpoint}") String address) {
        ClassLoader cargador = ProveedorApplication.class.getClassLoader();
        Endpoint endpoint = Endpoint.create(new ProveedorEndpoint(service));
        endpoint.setExecutor(Executors.newFixedThreadPool(8, tarea -> {
            Thread hilo = new Thread(tarea, "proveedor-soap");
            hilo.setContextClassLoader(cargador);
            hilo.setDaemon(true);
            return hilo;
        }));
        endpoint.publish(address);
        return endpoint;
    }
}
