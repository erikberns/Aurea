# Riesgos OWASP Top 10 para aplicaciones con LLM (2025) y su mitigación

| Riesgo | Cómo aplica al asistente | Mitigación | Evidencia |
| --- | --- | --- | --- |
| LLM01 Prompt injection | Un nombre o detalle con «ignorá las instrucciones…» | Los datos viajan delimitados y marcados como datos en el prompt; la salida pasa por el validador; el administrador revisa antes de guardar | Casos A01 y A09 del eval; `ValidadorSugerenciaProductoTests` |
| LLM02 Divulgación de información sensible | Que se envíen o devuelvan datos personales, o que se filtre la clave | Solo viajan datos del producto; el validador rechaza emails y teléfonos; la clave solo va en la cabecera y nunca a logs | Caso A06; `politica-de-datos.md` |
| LLM05 Manejo inadecuado de la salida | HTML, scripts o enlaces en la respuesta | El validador rechaza `<`, `>`, `http` y `www.`; el frontend muestra la respuesta como texto plano | Casos A03, A07 y A08 |
| LLM06 Exceso de autonomía | Que la IA guarde o publique cambios | La IA no tiene herramientas ni permisos: solo devuelve un texto que el administrador decide usar | Endpoint sin efectos; `AsistenteDescripcionIA.jsx` |
| LLM09 Desinformación | Precios, promociones o materiales inventados | El validador rechaza precios y promociones, y cualquier material o piedra que no esté en los datos | Caso A02; test de materiales inventados |
| LLM10 Consumo sin límite | Abuso que agote la cuota o el costo | Límite de 10 solicitudes por minuto por administrador (429), largos máximos de entrada, timeout de 8 s, circuit breaker y feature flag | `AIGatewayServiceTests` |

Los riesgos LLM03 (cadena de suministro), LLM04 (envenenamiento de datos), LLM07 (fuga del prompt de sistema) y LLM08 (embeddings) aplican poco: no se entrena ni se ajusta ningún modelo y no hay RAG. Igual, el prompt no contiene secretos y el validador rechaza respuestas que hablen de instrucciones o del prompt (caso A04).
