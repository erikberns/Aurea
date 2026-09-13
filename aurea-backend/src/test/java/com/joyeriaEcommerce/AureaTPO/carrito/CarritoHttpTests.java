package com.joyeriaEcommerce.AureaTPO.carrito;

import com.joyeriaEcommerce.AureaTPO.carrito.negocio.ServicioDeCarrito;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.ProductService;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.OrderRepository;
import com.joyeriaEcommerce.AureaTPO.ordenes.negocio.CheckoutMetricsState;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;

@SpringBootTest
class CarritoHttpTests {
 @Autowired WebApplicationContext context;
 @Autowired ProductService productos;
 @Autowired OrderRepository ordenes;
 @Autowired CheckoutMetricsState metricas;
 MockMvc mvc;
 @BeforeEach void setup(){mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 org.springframework.test.web.servlet.request.RequestPostProcessor token() throws Exception {
  String json=mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String valor=JsonPath.read(json,"$.token"),cabecera=JsonPath.read(json,"$.headerName");
  return request -> {
   request.setCookies(new jakarta.servlet.http.Cookie("XSRF-TOKEN",valor));
   request.addHeader(cabecera,valor); return request;
  };
 }
 long producto(){return productos.createProduct("Joya prueba", "Prueba", 10000.0, 5, null, "").id();}
 MockHttpSession sesion() throws Exception {
  return (MockHttpSession)mvc.perform(get("/api/carrito")).andExpect(status().isOk()).andReturn().getRequest().getSession();
 }
 void agregar(MockHttpSession s,long id,int cantidad) throws Exception {
  mvc.perform(post("/api/carrito/items").session(s).with(token()).contentType("application/json")
   .content("{\"productoId\":"+id+",\"cantidad\":"+cantidad+"}")).andExpect(status().isOk());
 }
 void registrar(MockHttpSession s) throws Exception {
  mvc.perform(post("/api/usuarios").session(s).with(token()).contentType("application/json")
   .content("{\"nombre\":\"Ana\",\"apellido\":\"Prueba\",\"email\":\""+UUID.randomUUID()+"@example.com\",\"contrasena\":\"ClaveSegura123\"}"))
   .andExpect(status().isCreated());
 }
 @Test void conservaEstadoEntrePeticionesYAislaSesiones() throws Exception {
  long id=producto(); MockHttpSession a=sesion(),b=sesion(); agregar(a,id,2);
  mvc.perform(get("/api/carrito").session(a)).andExpect(jsonPath("$.cantidadTotal").value(2));
  mvc.perform(get("/api/carrito").session(b)).andExpect(jsonPath("$.cantidadTotal").value(0));
  assertThat(a.getAttribute("scopedTarget.servicioDeCarrito")).isNotSameAs(b.getAttribute("scopedTarget.servicioDeCarrito"));
  assertThat(context.getBean(ProductService.class)).isSameAs(context.getBean(ProductService.class));
 }
 @Test void logoutDestruyeEstadoYCancelaAutenticacion() throws Exception {
  MockHttpSession a=sesion(); registrar(a); agregar(a,producto(),1);
  ServicioDeCarrito instancia=(ServicioDeCarrito)a.getAttribute("scopedTarget.servicioDeCarrito");
  mvc.perform(post("/api/usuarios/salir").session(a).with(token())).andExpect(status().isOk());
  assertThat(a.isInvalid()).isTrue();
  assertThatThrownBy(instancia::consultar).isInstanceOf(IllegalStateException.class);
  mvc.perform(get("/api/carrito")).andExpect(jsonPath("$.cantidadTotal").value(0));
  mvc.perform(get("/api/ordenes/mis-pedidos")).andExpect(status().isUnauthorized());
 }
 @Test void usaTokenCsrfRealYConservaCarritoInvitadoAlAutenticar() throws Exception {
  MockHttpSession a=sesion(); agregar(a,producto(),1); String anterior=a.getId();
  String respuesta=mvc.perform(get("/api/csrf").session(a)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String token=JsonPath.read(respuesta,"$.token");
  mvc.perform(post("/api/usuarios").session(a)
   .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN",token)).header("X-XSRF-TOKEN",token).contentType("application/json")
   .content("{\"nombre\":\"Ana\",\"apellido\":\"Real\",\"email\":\""+UUID.randomUUID()+"@example.com\",\"contrasena\":\"ClaveSegura123\"}"))
   .andExpect(status().isCreated());
  assertThat(a.getId()).isNotEqualTo(anterior);
  mvc.perform(get("/api/usuarios/sesion").session(a)).andExpect(jsonPath("$.nombre").value("Ana"));
  mvc.perform(get("/api/carrito").session(a)).andExpect(jsonPath("$.cantidadTotal").value(1));
 }
 @Test void compraAtomicaVaciaCarritoYDescuentaStock() throws Exception {
  long id=producto(); MockHttpSession a=sesion(); registrar(a); agregar(a,id,2);
  mvc.perform(post("/api/carrito/checkout").session(a).with(token()).contentType("application/json")
   .content("{\"direccion\":\"Calle 123\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(24500.0));
  assertThat(productos.getProductById(id).stock()).isEqualTo(3);
  mvc.perform(get("/api/carrito").session(a)).andExpect(jsonPath("$.cantidadTotal").value(0));
  mvc.perform(post("/api/carrito/checkout").session(a).with(token()).contentType("application/json")
   .content("{\"direccion\":\"Calle 123\"}")).andExpect(status().isBadRequest());
 }
 @Test void fallaDeStockConservaSeleccionSinPedidoNiMetricas() throws Exception {
  long id=producto(); MockHttpSession a=sesion(); registrar(a); agregar(a,id,2); productos.updateStock(id,1);
  long antes=ordenes.count(),confirmadas=metricas.getConfirmedOrdersCount();
  mvc.perform(post("/api/carrito/checkout").session(a).with(token()).contentType("application/json")
   .content("{\"direccion\":\"Calle 123\"}")).andExpect(status().isConflict());
  assertThat(ordenes.count()).isEqualTo(antes); assertThat(metricas.getConfirmedOrdersCount()).isEqualTo(confirmadas);
  assertThat(productos.getProductById(id).stock()).isEqualTo(1);
  mvc.perform(get("/api/carrito").session(a)).andExpect(jsonPath("$.cantidadTotal").value(2));
 }
 @Test void rechazaCantidadesInvalidasYExigeRolAdministrador() throws Exception {
  long id=producto(); MockHttpSession a=sesion(); registrar(a);
  mvc.perform(post("/api/carrito/items").session(a).with(token()).contentType("application/json")
   .content("{\"productoId\":"+id+",\"cantidad\":-1}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/ordenes").session(a).with(token()).contentType("application/json")
   .content("{\"shippingAddress\":\"Calle 123\",\"items\":[{\"productId\":"+id+",\"quantity\":-1}]}"))
   .andExpect(status().isBadRequest());
  mvc.perform(patch("/api/productos/"+id+"/precio").session(a).with(token()).param("newPrice","99"))
   .andExpect(status().isForbidden());
  mvc.perform(patch("/api/productos/"+id+"/precio").with(user("admin").roles("ADMIN")).with(token()).param("newPrice","99"))
   .andExpect(status().isOk());
  mvc.perform(patch("/api/productos/"+id+"/precio").with(user("admin").roles("ADMIN")).with(token()).param("newPrice","-1"))
   .andExpect(status().isBadRequest());
 }
 @Test void mutacionSinCsrfSeRechaza() throws Exception {
  mvc.perform(post("/api/carrito/items").contentType("application/json").content("{\"productoId\":1,\"cantidad\":1}"))
   .andExpect(status().isForbidden());
 }
}
