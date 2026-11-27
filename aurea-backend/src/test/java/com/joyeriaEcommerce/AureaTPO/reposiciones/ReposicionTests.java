package com.joyeriaEcommerce.AureaTPO.reposiciones;
import com.joyeriaEcommerce.AureaTPO.reposiciones.negocio.*;
import com.joyeriaEcommerce.AureaTPO.reposiciones.datos.*;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.*;
import com.joyeriaEcommerce.AureaTPO.notificaciones.negocio.*;
import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.IJwtService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ReposicionTests {
    @MockitoBean IProveedorGateway proveedor;
    @Autowired IReposiciones reposiciones;
    @Autowired IProductos productos;
    @Autowired INotificaciones notificaciones;
    @Autowired EventoPendienteRepository outbox;
    @Autowired ObjectMapper json;
    @Autowired WebApplicationContext context;
    @Autowired IJwtService jwt;
    @Autowired org.springframework.transaction.PlatformTransactionManager manager;
    private ReposicionDTO preparar(String estado) {
        long producto = productos.createProduct("Reposición test", "Plata", 100.0, 5, null, "").id();
        String id = UUID.randomUUID().toString();
        when(proveedor.consultar(id)).thenReturn(new ResultadoProveedorDTO(id, "ANILLO-PLATA", 3, estado, 0));
        return reposiciones.solicitar(id, producto, "ANILLO-PLATA", 3);
    }
    @Test void recibirGuardaOutboxSinModificarStockYAmbosConsumidoresSonIdempotentes() {
        var r = preparar("ACEPTADA");
        assertThat(productos.getProductById(r.productoId()).stock()).isEqualTo(5);
        reposiciones.recibir(r.id()); reposiciones.recibir(r.id());
        String payload = outbox.findById(r.id()).orElseThrow().payload;
        var inventario = new ReposicionInventarioListener(productos, json);
        var avisos = new ReposicionNotificacionListener(notificaciones, json);
        assertThat(productos.getProductById(r.productoId()).stock()).isEqualTo(5);
        inventario.recibir(payload); inventario.recibir(payload);
        avisos.recibir(payload); avisos.recibir(payload);
        assertThat(productos.getProductById(r.productoId()).stock()).isEqualTo(8);
        assertThat(notificaciones.listar().stream().filter(n -> n.id().equals(r.id())).count()).isEqualTo(1);
        assertThat(productos.reposicionAplicada(r.id())).isTrue();
    }
    @Test void rechazoNoPermiteRecepcionNiGeneraEvento() {
        var r = preparar("RECHAZADA");
        assertThatThrownBy(() -> reposiciones.recibir(r.id())).isInstanceOf(IllegalStateException.class);
        assertThat(outbox.existsById(r.id())).isFalse();
        assertThat(productos.getProductById(r.productoId()).stock()).isEqualTo(5);
    }
    @Test void timeoutSeRecuperaConsultandoMismoIdentificador() {
        long p = productos.createProduct("Timeout", "", 1.0, 0, null, "").id();
        String id = UUID.randomUUID().toString();
        when(proveedor.consultar(id)).thenThrow(new ProveedorNoDisponibleException("Timeout", null));
        assertThat(reposiciones.solicitar(id, p, "ANILLO-PLATA", 3).estado()).isEqualTo("SOLICITADA");
        doReturn(new ResultadoProveedorDTO(id, "ANILLO-PLATA", 3, "ACEPTADA", 0)).when(proveedor).consultar(id);
        assertThat(reposiciones.sincronizar(id).estado()).isEqualTo("ACEPTADA");
        verify(proveedor, never()).solicitar(eq(id), anyString(), anyInt());
        assertThatThrownBy(() -> reposiciones.solicitar(id, p, "ANILLO-PLATA", 4)).isInstanceOf(IllegalStateException.class);
    }
    @Test void solicitudNuevaConsultaAntesDeEnviar() {
        long p = productos.createProduct("Nuevo", "", 1.0, 0, null, "").id();
        String id = UUID.randomUUID().toString();
        when(proveedor.consultar(id)).thenReturn(new ResultadoProveedorDTO(id, "", 0, "NO_ENCONTRADA", 0));
        when(proveedor.solicitar(id, "ANILLO-PLATA", 2)).thenReturn(new ResultadoProveedorDTO(id, "ANILLO-PLATA", 2, "ACEPTADA", 0));
        assertThat(reposiciones.solicitar(id, p, "ANILLO-PLATA", 2).estado()).isEqualTo("ACEPTADA");
    }
    @Test void inventarioConcurrenteAplicaEventoUnaSolaVez() throws Exception {
        var r = preparar("ACEPTADA");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tareas = List.<Callable<Void>>of(
                () -> { productos.aplicarReposicion(r.id(), r.productoId(), 3); return null; },
                () -> { productos.aplicarReposicion(r.id(), r.productoId(), 3); return null; });
            for (var result : executor.invokeAll(tareas)) result.get();
        }
        assertThat(productos.getProductById(r.productoId()).stock()).isEqualTo(8);
    }
    @Test void falloDeInventarioNoMarcaEventoComoAplicado() {
        String id = UUID.randomUUID().toString();
        assertThatThrownBy(() -> productos.aplicarReposicion(id, Long.MAX_VALUE, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThat(productos.reposicionAplicada(id)).isFalse();
    }
    @Test void outboxConservaEventoSiBrokerFallaYMarcaPublicadoSoloConConfirmacion() {
        String id = UUID.randomUUID().toString();
        outbox.save(new EventoPendiente(id, "{}"));
        var rabbit = mock(org.springframework.amqp.rabbit.core.RabbitTemplate.class);
        doThrow(new org.springframework.amqp.AmqpConnectException(new java.net.ConnectException("Broker caído")))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class),
                    any(org.springframework.amqp.core.MessagePostProcessor.class),
                    any(org.springframework.amqp.rabbit.connection.CorrelationData.class));
        var publisher = new ReposicionOutboxPublisher(outbox, rabbit, manager);
        publisher.publicarPendientes();
        var pendiente = outbox.findById(id).orElseThrow();
        assertThat(pendiente.publicado).isNull();
        assertThat(pendiente.intentos).isEqualTo(1);
        pendiente.proximoIntento = java.time.Instant.now().minusSeconds(1);
        outbox.save(pendiente);
        doAnswer(invocation -> {
            org.springframework.amqp.rabbit.connection.CorrelationData correlacion = invocation.getArgument(4);
            correlacion.getFuture().complete(new org.springframework.amqp.rabbit.connection.CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class),
                any(org.springframework.amqp.core.MessagePostProcessor.class),
                any(org.springframework.amqp.rabbit.connection.CorrelationData.class));
        publisher.publicarPendientes();
        assertThat(outbox.findById(id).orElseThrow().publicado).isNotNull();
    }
    @Test void endpointsExigenAdministradorYValidanEntrada() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/api/reposiciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notificaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reposiciones").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("cliente").roles("CLIENTE")))
                .andExpect(status().isForbidden());
        String token = "Bearer " + jwt.generateToken("admin@aurea.com", "ROLE_ADMIN");
        mvc.perform(get("/api/reposiciones").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(post("/api/reposiciones").header("Authorization", token).contentType("application/json")
                .content("{\"idSolicitud\":\"x\",\"productoId\":1,\"codigoProveedor\":\"ANILLO-PLATA\",\"cantidad\":0}"))
                .andExpect(status().isBadRequest());
    }
    @Test void solicitudValidaIdentificadorCodigoYCantidad() {
        long p = productos.createProduct("Validaciones", "", 1.0, 0, null, "").id();
        String id = UUID.randomUUID().toString();
        assertThatThrownBy(() -> reposiciones.solicitar("no-uuid", p, "ANILLO-PLATA", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reposiciones.solicitar(id, p, "ANILLO-PLATA", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reposiciones.solicitar(id, p, "ANILLO-PLATA", 10001)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reposiciones.solicitar(id, p, null, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reposiciones.solicitar(id, p, "anillo plata", 1)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(proveedor);
    }
    @Test void identificadorReutilizadoConOtroProductoOCodigoSeRechaza() {
        var r = preparar("ACEPTADA");
        long otro = productos.createProduct("Otro", "", 1.0, 0, null, "").id();
        assertThatThrownBy(() -> reposiciones.solicitar(r.id(), otro, "ANILLO-PLATA", 3)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> reposiciones.solicitar(r.id(), r.productoId(), "COLLAR-ORO", 3)).isInstanceOf(IllegalStateException.class);
        assertThat(reposiciones.solicitar(r.id(), r.productoId(), "ANILLO-PLATA", 3).estado()).isEqualTo("ACEPTADA");
    }
    @Test void respuestaIncompatibleDelProveedorDejaLaSolicitudPendienteConDetalle() {
        long p = productos.createProduct("Incompatible", "", 1.0, 0, null, "").id();
        String id = UUID.randomUUID().toString();
        when(proveedor.consultar(id)).thenReturn(new ResultadoProveedorDTO(id, "ANILLO-PLATA", 99, "ACEPTADA", 0));
        var r = reposiciones.solicitar(id, p, "ANILLO-PLATA", 3);
        assertThat(r.estado()).isEqualTo("SOLICITADA");
        assertThat(r.detalle()).contains("incompatibles");
        doReturn(new ResultadoProveedorDTO(UUID.randomUUID().toString(), "ANILLO-PLATA", 3, "ACEPTADA", 0)).when(proveedor).consultar(id);
        assertThat(reposiciones.sincronizar(id).estado()).isEqualTo("SOLICITADA");
        doReturn(new ResultadoProveedorDTO(id, "COLLAR-ORO", 3, "ACEPTADA", 0)).when(proveedor).consultar(id);
        assertThat(reposiciones.sincronizar(id).estado()).isEqualTo("SOLICITADA");
        doReturn(new ResultadoProveedorDTO(id, "ANILLO-PLATA", 3, "EN_PROCESO", 0)).when(proveedor).consultar(id);
        assertThat(reposiciones.sincronizar(id).estado()).isEqualTo("SOLICITADA");
        doReturn(new ResultadoProveedorDTO(id, "ANILLO-PLATA", 3, "RECHAZADA", 0)).when(proveedor).consultar(id);
        var rechazada = reposiciones.sincronizar(id);
        assertThat(rechazada.estado()).isEqualTo("RECHAZADA");
        assertThat(rechazada.detalle()).isNull();
    }
    @Test void sincronizarUnaSolicitudYaResueltaNoLlamaAlProveedor() {
        var r = preparar("ACEPTADA");
        clearInvocations(proveedor);
        assertThat(reposiciones.sincronizar(r.id()).estado()).isEqualTo("ACEPTADA");
        verifyNoInteractions(proveedor);
    }
    @Test void recibirUnaSolicitudPendienteOInexistenteFalla() {
        long p = productos.createProduct("Pendiente", "", 1.0, 0, null, "").id();
        String id = UUID.randomUUID().toString();
        when(proveedor.consultar(id)).thenThrow(new ProveedorNoDisponibleException("Caído", null));
        assertThat(reposiciones.solicitar(id, p, "ANILLO-PLATA", 1).detalle()).isEqualTo("Caído");
        assertThatThrownBy(() -> reposiciones.recibir(id)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> reposiciones.recibir(UUID.randomUUID().toString())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> reposiciones.sincronizar(UUID.randomUUID().toString())).isInstanceOf(RuntimeException.class);
    }
}
