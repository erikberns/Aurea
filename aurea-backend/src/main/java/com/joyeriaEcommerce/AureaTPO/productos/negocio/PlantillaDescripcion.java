package com.joyeriaEcommerce.AureaTPO.productos.negocio;

import java.util.ArrayList;
import java.util.List;

/**
 * Fallback a modo manual: arma una descripción base con los datos del administrador, sin IA,
 * para que la complete a mano. Si un dato no es seguro de mostrar (por ejemplo, trae HTML o
 * una instrucción), no se copia.
 */
public final class PlantillaDescripcion {

    private PlantillaDescripcion() {
    }

    public static String generar(String nombre, String material) {
        String pieza = textoSeguro(nombre, 80);
        String materialSeguro = textoSeguro(material, 60);
        StringBuilder texto = new StringBuilder(pieza.isBlank() ? "Esta pieza" : pieza);
        if (!materialSeguro.isBlank()) texto.append(", en ").append(materialSeguro).append(',');
        texto.append(" forma parte de la colección Áurea. Completá esta descripción con la terminación, ")
                .append("las piedras y el concepto de la joya antes de publicarla.");
        String resultado = texto.toString();
        return resultado.length() <= ValidadorSugerenciaProducto.LARGO_MAXIMO ? resultado
                : resultado.substring(0, ValidadorSugerenciaProducto.LARGO_MAXIMO - 1) + "…";
    }

    /** Categoría cuya palabra clave aparece en el nombre (comparando la raíz), o null si no hay una clara. */
    public static CategoryDTO categoriaPorNombre(String nombre, List<CategoryDTO> categorias) {
        List<String> palabras = raices(nombre);
        for (CategoryDTO categoria : categorias) {
            for (String raiz : raices(categoria.description())) {
                if (palabras.contains(raiz)) return categoria;
            }
        }
        return null;
    }

    private static String textoSeguro(String valor, int maximo) {
        if (valor == null) return "";
        String limpio = valor.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").strip();
        if (limpio.isEmpty() || !ValidadorSugerenciaProducto.esSegura(limpio) || limpio.matches(".*[{}\\[\\]`].*")) {
            return "";
        }
        if (limpio.length() <= maximo) return limpio;
        String corte = limpio.substring(0, maximo);
        int espacio = corte.lastIndexOf(' ');
        return (espacio > maximo / 2 ? corte.substring(0, espacio) : corte).strip();
    }

    private static List<String> raices(String texto) {
        List<String> raices = new ArrayList<>();
        for (String palabra : ValidadorSugerenciaProducto.normalizar(texto).split("[^a-z]+")) {
            if (palabra.length() >= 4) raices.add(palabra.substring(0, Math.min(5, palabra.length())));
        }
        return raices;
    }
}
