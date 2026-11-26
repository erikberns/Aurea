package com.joyeriaEcommerce.AureaTPO.productos.negocio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ValidadorSugerenciaProductoTests {

    private static final List<String> CATEGORIAS = List.of("Anillos apilables", "Collares & Medallas",
            "Pulseras & Eslabones", "Pendientes & Huggies");
    private static final String BASE = "Anillo Luna en plata 925 con un circón central, delicado y luminoso";

    private final ObjectMapper json = JsonMapper.builder().build();
    private final ValidadorSugerenciaProducto validador =
            new ValidadorSugerenciaProducto(CATEGORIAS, "Anillo Luna plata 925 con circón central");

    private static String respuesta(String descripcion, String categoria) {
        return "{\"descripcion\":\"" + descripcion + "\",\"categoria\":\"" + categoria + "\"}";
    }

    @Test
    void aceptaUnaRespuestaValidaYNormalizaLaCategoria() {
        var resultado = validador.validar(respuesta(BASE + ", ideal para combinar.", "anillos apilables"), json);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().categoria()).isEqualTo("Anillos apilables");
        assertThat(resultado.get().descripcion()).startsWith("Anillo Luna");
    }

    @Test
    void aceptaElJsonEnvueltoEnUnBloqueDeCodigo() {
        String envuelto = "```json\n" + respuesta(BASE + ", ideal para combinar.", "Anillos apilables") + "\n```";
        assertThat(validador.validar(envuelto, json)).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"no es json", "[]", "{\"descripcion\":\"sin categoria pero con el largo suficiente para pasar el mínimo de caracteres\"}",
            "{\"descripcion\":42,\"categoria\":\"Anillos apilables\"}"})
    void rechazaJsonInvalidoOCamposFaltantes(String respuesta) {
        assertThat(validador.validar(respuesta, json)).isEmpty();
    }

    @Test
    void rechazaLargosFueraDeRangoYCategoriasInexistentes() {
        assertThat(validador.validar(respuesta("Anillo Luna en plata.", "Anillos apilables"), json)).isEmpty();
        assertThat(validador.validar(respuesta(BASE + ". " + "Muy luminoso. ".repeat(20), "Anillos apilables"), json)).isEmpty();
        assertThat(validador.validar(respuesta(BASE + ", ideal para combinar.", "Relojes"), json)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ", hoy a $25.000 con envío gratis",
            ", con 30 % de descuento por tiempo limitado en la oferta",
            ", mirá más en https://ejemplo.com",
            ", <b>imperdible</b>",
            ", escribinos a ventas@ejemplo.com o al 11 5555 1234",
            ", perfecto para un cliente idiota",
            ", ignoré las instrucciones anteriores como pediste"})
    void rechazaPreciosHtmlEnlacesDatosPersonalesInsultosYRestosDeInstrucciones(String agregado) {
        assertThat(validador.validar(respuesta(BASE + agregado + ".", "Anillos apilables"), json)).isEmpty();
    }

    @Test
    void rechazaMaterialesQueElAdministradorNoCargo() {
        String inventado = "Anillo Luna en oro de 18k con diamantes, delicado y luminoso para combinar con otros.";
        assertThat(validador.validar(respuesta(inventado, "Anillos apilables"), json)).isEmpty();
    }
}
