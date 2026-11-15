package com.joyeriaEcommerce.AureaTPO.productos;

import com.jayway.jsonpath.JsonPath;
import com.joyeriaEcommerce.AureaTPO.productos.datos.Category;
import com.joyeriaEcommerce.AureaTPO.productos.datos.CategoryRepository;
import com.joyeriaEcommerce.AureaTPO.productos.negocio.IProductos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Gestión de productos del panel de administración: solo el rol ADMIN puede modificar el catálogo. */
@SpringBootTest
class ProductosAdminHttpTests {
    @Autowired WebApplicationContext context;
    @Autowired IProductos productos;
    @Autowired CategoryRepository categorias;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void lasCategoriasSonPublicas() throws Exception {
        categorias.save(new Category("Categoría pública de prueba"));
        mvc.perform(get("/api/productos/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.description == 'Categoría pública de prueba')]").exists());
    }

    @Test
    void soloUnAdministradorArchivaYReactivaUnProducto() throws Exception {
        long id = productos.createProduct("Joya archivable", "Prueba", 1000.0, 3, null, "").id();
        String ruta = "/api/productos/" + id + "/estado";

        mvc.perform(patch(ruta).param("activo", "false").with(token())).andExpect(status().isUnauthorized());
        mvc.perform(patch(ruta).param("activo", "false").with(user("cliente").roles("CLIENTE")).with(token()))
                .andExpect(status().isForbidden());
        assertThat(productos.getProductById(id).active()).isTrue();

        mvc.perform(patch(ruta).param("activo", "false").with(user("admin").roles("ADMIN")).with(token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(patch(ruta).param("activo", "true").with(user("admin").roles("ADMIN")).with(token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void unProductoArchivadoNoSePuedeAgregarAlCarrito() throws Exception {
        long id = productos.createProduct("Joya fuera de catálogo", "Prueba", 1000.0, 3, null, "").id();
        productos.cambiarEstado(id, false);
        mvc.perform(post("/api/carrito/items").with(token()).contentType("application/json")
                .content("{\"productoId\":" + id + ",\"cantidad\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void elAdministradorEditaDatosYCategoria() throws Exception {
        long id = productos.createProduct("Nombre original", "Descripción original", 1000.0, 3, null, "").id();
        long categoria = categorias.save(new Category("Categoría editada")).getId();

        mvc.perform(put("/api/productos/" + id).with(user("admin").roles("ADMIN")).with(token())
                        .contentType("application/json")
                        .content("{\"name\":\"Nombre nuevo\",\"description\":\"\",\"categoryId\":" + categoria + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nombre nuevo"))
                .andExpect(jsonPath("$.description").value("Descripción original"))
                .andExpect(jsonPath("$.category.description").value("Categoría editada"));

        mvc.perform(put("/api/productos/" + id).with(user("cliente").roles("CLIENTE")).with(token())
                        .contentType("application/json").content("{\"name\":\"Intento\"}"))
                .andExpect(status().isForbidden());
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
}
