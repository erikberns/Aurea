# Ficha de la funcionalidad: asistente de descripciones con IA

| Campo | Detalle |
| --- | --- |
| Nombre | Asistente de descripciones de productos |
| Usuario | Administrador de Áurea, en el formulario de alta y edición de joyas |
| Problema | Redactar fichas de producto consistentes lleva tiempo y queda desparejo entre productos |
| Qué hace | A partir del nombre, el material y los detalles, sugiere una descripción comercial (60 a 255 caracteres) y una de las categorías existentes |
| Qué no hace | No guarda nada, no publica, no fija precios ni stock y no tiene acceso a datos de clientes ni a otras herramientas |
| Humano en el circuito | La sugerencia se muestra marcada «Generado con IA»; el administrador la revisa, la edita y decide si la usa |
| Modelo | Google Gemini por la API REST generateContent (modelo configurable con IA_MODELO); modelo simulado sin red por defecto. Ver ADR-009 |
| Entrada | Nombre (hasta 120 caracteres), material (hasta 80) y detalles (hasta 400), más la lista de categorías |
| Salida | JSON `{"descripcion", "categoria"}`, validado antes de mostrarse |
| Prompt | `aurea-backend/src/main/resources/prompts/descripcion-producto.v1.md` (versionado; la versión queda en cada traza) |
| Fallback | Plantilla sin IA con los datos seguros del administrador, para completar a mano |
| Endpoint | `POST /api/productos/sugerencia-ia` (solo ADMIN); estado en `GET /api/productos/sugerencia-ia/estado`; trazas en `GET /api/ia/trazas` |
| Métricas y umbrales | Seguridad 100 %, adversariales bloqueados 100 %, calidad ≥ 90 %, valor ≥ 80 %, latencia p95 ≤ 8 s, 0 errores. Ver `aurea-backend/evals/` |
| Riesgos | Ver `riesgos-owasp-llm.md` |
| Datos | Ver `politica-de-datos.md` |
| Apagado | Ver `plan-de-apagado.md` |
| Responsable | A completar por el equipo |

## Flujo

1. El administrador completa el nombre, el material y los detalles, y aprieta «Sugerir con IA».
2. `SugerenciaProductoService` recorta los datos, arma el prompt con las categorías y delimita los datos entre `<datos_producto>` y `</datos_producto>`.
3. `IAGateway` revisa el feature flag, el límite por usuario (10 por minuto) y el circuit breaker, y llama al modelo con timeout de 8 s.
4. `ValidadorSugerenciaProducto` acepta la respuesta solo si cumple el contrato y las reglas de contenido.
5. Si es válida, se devuelve con `fuente: "ia"`; si no, se devuelve la plantilla con `fuente: "plantilla"` y el motivo.
6. El gateway registra la traza: versión del prompt, modelo, tokens, costo estimado, latencia y resultado.

## Limitaciones conocidas

- El modelo simulado no mide la calidad de redacción de un modelo real: para eso se corre el eval contra Gemini.
- El límite por usuario y el estado del circuito viven en memoria (una sola instancia del backend).
- La lista de materiales que el modelo no puede inventar es fija y puede no cubrir todos los casos.
