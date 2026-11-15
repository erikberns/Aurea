package com.joyeriaEcommerce.AureaTPO.reposiciones;

import com.joyeriaEcommerce.AureaTPO.reposiciones.negocio.IReposiciones;
import com.joyeriaEcommerce.AureaTPO.reposiciones.datos.EventoPendienteRepository;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.IProductos;
import com.joyeriaEcommerce.AureaTPO.notificaciones.negocio.INotificaciones;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.ICheckout;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.IPedidos;
import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.IJwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Duration;
import java.util.UUID;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Opt-in: requiere proveedor separado en 18091 y docker-compose.integration.yml. */
@EnabledIfSystemProperty(named = "aurea.integration", matches = "true")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:55433/aurea_integration",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.datasource.username=aurea_test", "spring.datasource.password=aurea_test",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.rabbitmq.host=localhost", "spring.rabbitmq.port=5673",
    "spring.rabbitmq.username=aurea_test", "spring.rabbitmq.password=aurea_test",
    "spring.rabbitmq.listener.simple.auto-startup=true",
    "spring.rabbitmq.listener.simple.retry.enabled=true",
    "spring.rabbitmq.listener.simple.retry.max-attempts=3",
    "spring.rabbitmq.listener.simple.retry.initial-interval=100ms",
    "spring.rabbitmq.listener.simple.default-requeue-rejected=false",
    "spring.rabbitmq.publisher-confirm-type=correlated", "spring.rabbitmq.publisher-returns=true",
    "spring.rabbitmq.template.mandatory=true",
    "app.messaging.reposiciones.enabled=true", "app.messaging.order-processing.enabled=true",
    "app.messaging.outbox.delay-ms=300",
    "app.proveedor.url=http://127.0.0.1:18091/soap/proveedor", "app.proveedor.timeout-ms=3000"
})
class IntegracionRealTests {
    @Autowired IReposiciones reposiciones;
    @Autowired IProductos productos;
    @Autowired INotificaciones notificaciones;
    @Autowired EventoPendienteRepository outbox;
    @Autowired RabbitTemplate rabbit;
    @Autowired WebApplicationContext context;
    @Autowired IJwtService jwt;
    @Autowired ICheckout checkout;
    @Autowired IPedidos pedidos;

    @Test void restSoapPostgresRabbitDosSuscriptoresYColaDePedidos() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String bearer = "Bearer " + jwt.generateToken("admin@aurea.com", "ROLE_ADMIN");
        long productoId = productos.createProduct("Integración real", "Prueba", 100.0, 10, null, "").id();
        String id = UUID.randomUUID().toString();
        String solicitud = "{\"idSolicitud\":\"" + id + "\",\"productoId\":" + productoId
                + ",\"codigoProveedor\":\"ANILLO-PLATA\",\"cantidad\":7}";
        mvc.perform(post("/api/reposiciones").header("Authorization", bearer).contentType("application/json").content(solicitud))
                .andExpect(status().isAccepted());
        assertThat(reposiciones.listar().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow().estado()).isEqualTo("ACEPTADA");
        assertThat(productos.getProductById(productoId).stock()).isEqualTo(10);
        mvc.perform(post("/api/reposiciones/" + id + "/recibir").header("Authorization", bearer)).andExpect(status().isOk());
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(productos.getProductById(productoId).stock()).isEqualTo(17);
            assertThat(notificaciones.listar().stream().filter(n -> n.id().equals(id)).count()).isEqualTo(1);
            assertThat(outbox.findById(id).orElseThrow().publicado).isNotNull();
        });
        String payload = outbox.findById(id).orElseThrow().payload;
        rabbit.convertAndSend("aurea.reposiciones", "reposicion.recibida", payload);
        rabbit.convertAndSend("aurea.reposiciones", "reposicion.recibida", payload);
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(productos.getProductById(productoId).stock()).isEqualTo(17);
            assertThat(notificaciones.listar().stream().filter(n -> n.id().equals(id)).count()).isEqualTo(1);
        });
        // El proceso punto a punto anterior también sigue funcionando con el broker real.
        var pedido = checkout.purchase("admin@aurea.com", "Dirección de prueba", Map.of(productoId, 1));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(pedidos.consultarPedido(pedido.id()).status()).isEqualTo("COMPLETED"));
        assertThat(productos.getProductById(productoId).stock()).isEqualTo(16);
    }

    @Test void mensajeInvalidoTerminaEnAmbasColasDeErrores() {
        rabbit.convertAndSend("aurea.reposiciones", "reposicion.recibida", "mensaje-invalido");
        await().atMost(Duration.ofSeconds(15)).until(() -> rabbit.receive("aurea.inventario.reposiciones.dlq") != null);
        await().atMost(Duration.ofSeconds(15)).until(() -> rabbit.receive("aurea.notificaciones.reposiciones.dlq") != null);
    }
}
