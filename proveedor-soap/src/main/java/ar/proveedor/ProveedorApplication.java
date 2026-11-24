package ar.proveedor;

import jakarta.xml.ws.Endpoint;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@SpringBootApplication
public class ProveedorApplication {
    public static void main(String[] args) { SpringApplication.run(ProveedorApplication.class, args); }

    @Bean(destroyMethod = "stop")
    @ConditionalOnProperty(name = "proveedor.soap.enabled", havingValue = "true", matchIfMissing = true)
    Endpoint endpoint(ProveedorService service, @Value("${proveedor.endpoint}") String address) {
        return Endpoint.publish(address, new ProveedorEndpoint(service));
    }
}
