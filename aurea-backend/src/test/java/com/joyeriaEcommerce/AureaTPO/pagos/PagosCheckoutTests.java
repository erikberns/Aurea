package com.joyeriaEcommerce.AureaTPO.pagos;

import com.jayway.jsonpath.JsonPath;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderRepository;
import com.joyeriaEcommerce.AureaTPO.pagos.negocio.IPagos;
import com.joyeriaEcommerce.AureaTPO.pagos.negocio.PagoDTO;
import com.joyeriaEcommerce.AureaTPO.pagos.negocio.PasarelaSimuladaAdapter;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.IProductos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Flujo crítico transaccional: pedido + pago (autorizar, confirmar, capturar) con la pasarela simulada. */
@SpringBootTest
class PagosCheckoutTests {
    @Autowired WebApplicationContext context;
    @Autowired IProductos productos;
    @Autowired OrderRepository ordenes;
    @Autowired IPagos pagos;
    @Autowired PasarelaSimuladaAdapter pasarela;
    @Autowired PlatformTransactionManager manager;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void tarjetaAprobadaConfirmaElPedidoYCapturaDespuesDelCommit() throws Exception {
        long producto = producto();
        MockHttpSession sesion = sesion();
        registrar(sesion);
        agregar(sesion, producto);
        String respuesta = comprar(sesion, "VISA_APROBADA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn().getResponse().getContentAsString();
        long orden = ((Number) JsonPath.read(respuesta, "$.id")).longValue();

        var registro = pagos.pagosDeOrden(orden);
        assertThat(registro).hasSize(1);
        PagoDTO pago = registro.get(0);
        assertThat(pago.estado()).isEqualTo("CAPTURADO");
        assertThat(pago.proveedor()).isEqualTo("simulado");
        assertThat(pago.montoCentavos()).isEqualTo(1_450_000L); // 10.000 + envío 4.500, en centavos
        assertThat(pasarela.operaciones()).contains("autorizar:" + orden, "capturar:" + pago.referenciaExterna());
        assertThat(productos.getProductById(producto).stock()).isEqualTo(4);
    }

    @Test
    void tarjetaRechazadaRespondePaymentRequiredYRevierteTodaLaCompra() throws Exception {
        long producto = producto();
        MockHttpSession sesion = sesion();
        registrar(sesion);
        agregar(sesion, producto);
        long antes = ordenes.count();

        comprar(sesion, "FONDOS_INSUFICIENTES")
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.mensaje").value(containsString("Fondos insuficientes")));

        assertThat(ordenes.count()).isEqualTo(antes);
        assertThat(productos.getProductById(producto).stock()).isEqualTo(5);
        mvc.perform(get("/api/carrito").session(sesion)).andExpect(jsonPath("$.cantidadTotal").value(1));
        assertThat(pagos.listar()).anySatisfy(p -> {
            assertThat(p.estado()).isEqualTo("RECHAZADO");
            assertThat(p.metodo()).isEqualTo("FONDOS_INSUFICIENTES");
        });
    }

    @Test
    void unaFallaPosteriorALaAutorizacionAnulaLaRetencionYNoCobra() {
        long orden = 900_000L + (long) (Math.random() * 1_000_000);
        AtomicReference<PagoDTO> autorizado = new AtomicReference<>();
        TransactionTemplate compra = new TransactionTemplate(manager);

        assertThatThrownBy(() -> compra.executeWithoutResult(estado -> {
            autorizado.set(pagos.autorizar(orden, 1000.0, "VISA_APROBADA"));
            throw new IllegalStateException("Falla simulada al confirmar el pedido");
        })).isInstanceOf(IllegalStateException.class);

        String referencia = autorizado.get().referenciaExterna();
        assertThat(pagos.pagosDeOrden(orden)).singleElement()
                .satisfies(p -> assertThat(p.estado()).isEqualTo("ANULADO"));
        assertThat(pasarela.operaciones()).contains("cancelar:" + referencia)
                .doesNotContain("capturar:" + referencia);
    }

    @Test
    void autorizarExigeUnaTransaccionDeCompraActiva() {
        assertThatThrownBy(() -> pagos.autorizar(1L, 100.0, "VISA_APROBADA"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void metodoDesconocidoEsRechazadoYLaAuditoriaRequiereAdministrador() throws Exception {
        long producto = producto();
        MockHttpSession sesion = sesion();
        registrar(sesion);
        agregar(sesion, producto);
        long antes = ordenes.count();

        comprar(sesion, "4242424242424242").andExpect(status().isBadRequest());
        assertThat(ordenes.count()).isEqualTo(antes);

        mvc.perform(get("/api/pagos/configuracion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proveedor").value("simulado"))
                .andExpect(jsonPath("$.metodos.length()").value(4));
        mvc.perform(get("/api/pagos/configuracion")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/pagos").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(get("/api/pagos").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    private ResultActions comprar(MockHttpSession sesion, String metodo) throws Exception {
        return mvc.perform(post("/api/carrito/checkout").session(sesion).with(token())
                .contentType("application/json")
                .content("{\"direccion\":\"Calle 123\",\"metodoPago\":\"" + metodo + "\"}"));
    }

    private RequestPostProcessor token() throws Exception {
        String json = mvc.perform(get("/api/csrf")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String valor = JsonPath.read(json, "$.token");
        String cabecera = JsonPath.read(json, "$.headerName");
        return request -> {
            request.setCookies(new jakarta.servlet.http.Cookie("XSRF-TOKEN", valor));
            request.addHeader(cabecera, valor);
            return request;
        };
    }

    private long producto() {
        return productos.createProduct("Joya pago", "Prueba de pago", 10000.0, 5, null, "").id();
    }

    private MockHttpSession sesion() throws Exception {
        return (MockHttpSession) mvc.perform(get("/api/carrito")).andExpect(status().isOk())
                .andReturn().getRequest().getSession();
    }

    private void agregar(MockHttpSession sesion, long producto) throws Exception {
        mvc.perform(post("/api/carrito/items").session(sesion).with(token()).contentType("application/json")
                .content("{\"productoId\":" + producto + ",\"cantidad\":1}")).andExpect(status().isOk());
    }

    private void registrar(MockHttpSession sesion) throws Exception {
        mvc.perform(post("/api/usuarios").session(sesion).with(token()).contentType("application/json")
                .content("{\"nombre\":\"Pago\",\"apellido\":\"Prueba\",\"email\":\"" + UUID.randomUUID()
                        + "@example.com\",\"contrasena\":\"ClaveSegura123\"}"))
                .andExpect(status().isCreated());
    }
}
