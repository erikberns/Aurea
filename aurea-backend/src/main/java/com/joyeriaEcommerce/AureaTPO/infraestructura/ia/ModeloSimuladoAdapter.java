package com.joyeriaEcommerce.AureaTPO.infraestructura.ia;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

/**
 * Modelo simulado, sin red ni clave: es el proveedor por defecto (docker compose y CI).
 *
 * <p>Imita a propósito a un modelo "ingenuo": arma la descripción copiando los datos que recibe,
 * sin filtrar nada. Así, si los datos traen una inyección, HTML o un precio, la respuesta también
 * los trae, y el eval set puede comprobar que las validaciones del negocio los detienen.</p>
 */
public class ModeloSimuladoAdapter implements ProveedorModeloIA {

    private static final String INICIO = "<datos_producto>";
    private static final String FIN = "</datos_producto>";

    private final ObjectMapper json;

    public ModeloSimuladoAdapter(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public String nombre() {
        return "simulado";
    }

    @Override
    public String modelo() {
        return "simulado-v1";
    }

    @Override
    public RespuestaModelo generar(SolicitudModelo solicitud) {
        Map<String, Object> datos = datos(solicitud.entrada());
        String nombre = texto(datos.get("nombre"));
        String material = texto(datos.get("material"));
        String detalles = texto(datos.get("detalles"));
        List<String> categorias = new ArrayList<>();
        if (datos.get("categoriasPermitidas") instanceof List<?> lista) {
            lista.forEach(c -> categorias.add(String.valueOf(c)));
        }

        StringBuilder descripcion = new StringBuilder(nombre.isBlank() ? "Pieza" : nombre);
        if (!material.isBlank()) descripcion.append(", en ").append(material);
        descripcion.append(". ");
        if (!detalles.isBlank()) {
            descripcion.append(Character.toUpperCase(detalles.charAt(0))).append(detalles.substring(1));
            if (!detalles.endsWith(".")) descripcion.append('.');
            descripcion.append(' ');
        }
        descripcion.append("Una pieza del taller de Áurea pensada para acompañarte todos los días.");

        Map<String, Object> salida = new LinkedHashMap<>();
        salida.put("descripcion", descripcion.toString());
        salida.put("categoria", categoriaPorNombre(nombre, categorias));
        String texto = json.writeValueAsString(salida);
        int tokensEntrada = (solicitud.instrucciones().length() + solicitud.entrada().length()) / 4;
        return new RespuestaModelo(texto, tokensEntrada, texto.length() / 4);
    }

    private Map<String, Object> datos(String entrada) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        int desde = entrada == null ? -1 : entrada.indexOf(INICIO);
        int hasta = entrada == null ? -1 : entrada.lastIndexOf(FIN);
        if (desde < 0 || hasta <= desde) return resultado;
        try {
            Object leido = json.readValue(entrada.substring(desde + INICIO.length(), hasta).trim(), Map.class);
            if (leido instanceof Map<?, ?> mapa) mapa.forEach((k, v) -> resultado.put(String.valueOf(k), v));
        } catch (RuntimeException ignorada) {
            // Entrada ilegible: se responde con una descripción genérica.
        }
        return resultado;
    }

    /** Elige la categoría cuya palabra clave aparece en el nombre (por raíz), o la primera. */
    private static String categoriaPorNombre(String nombre, List<String> categorias) {
        if (categorias.isEmpty()) return "";
        List<String> palabras = raices(nombre);
        for (String categoria : categorias) {
            for (String raiz : raices(categoria)) {
                if (palabras.contains(raiz)) return categoria;
            }
        }
        return categorias.get(0);
    }

    private static List<String> raices(String texto) {
        List<String> raices = new ArrayList<>();
        String normal = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        for (String palabra : normal.split("[^a-z]+")) {
            if (palabra.length() >= 4) raices.add(palabra.substring(0, Math.min(5, palabra.length())));
        }
        return raices;
    }

    private static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor).trim();
    }
}
