# ADR-009: Gateway de IA para el asistente de descripciones (Gemini, con modelo simulado por defecto)
Estado: implementada.

## Contexto
La Clase 14 pide al menos una funcionalidad de IA integrada al producto, con el modelo detrás de un gateway, prompts versionados, salida validada, timeout, circuit breaker, fallback a modo manual, trazas, un eval set y un plan de apagado. Además, el sistema tiene que seguir levantando con un solo comando y sin secretos (G1), y el CI no puede depender de un servicio pago ni de una clave.

## Decisión
La funcionalidad es un asistente que sugiere la descripción y la categoría de una joya en el formulario del administrador. Solo sugiere: el administrador revisa, edita y decide si la usa (humano en el circuito). Solo viajan datos del producto, nunca datos personales.

El modelo queda detrás de `IAGateway` (Facade) en `infraestructura/ia`, que es la única pieza que habla con un modelo, a través del puerto `ProveedorModeloIA` (Adapter). El gateway aplica, en este orden: feature flag (IA_HABILITADA), límite de solicitudes por usuario, circuit breaker (ADR-010), timeout de 5 s y la validación que le pasa la funcionalidad; y registra una traza por llamada en `ia_traza` con versión del prompt, modelo, tokens, costo estimado y latencia, sin guardar el texto.

Hay dos adapters. `GeminiModeloAdapter` usa la API REST generateContent de Google con la clave en la cabecera x-goog-api-key, como el adapter de Stripe usa la API HTTP sin SDK. `ModeloSimuladoAdapter` no usa red y responde copiando los datos sin filtrar, a propósito, para que el eval set pruebe que las validaciones detienen inyecciones, HTML, precios o datos personales. IA_PROVEEDOR=auto (por defecto) usa Gemini si hay IA_API_KEY y, si no, el simulado: para activar la IA real solo hace falta la clave.

El negocio (`SugerenciaProductoService`, en Productos) arma el pedido con el prompt versionado `prompts/descripcion-producto.v1.md`, delimita los datos del administrador entre etiquetas y los marca como datos, valida la respuesta con `ValidadorSugerenciaProducto` y, si no hay una respuesta válida, devuelve una plantilla armada sin IA (`PlantillaDescripcion`).

## Alternativas
Usar el SDK de Google ocultaría el protocolo y agregaría una dependencia para una sola llamada. Llamar al modelo desde el servicio de productos acoplaría el negocio al proveedor y repartiría el manejo de fallas. OpenAI o Anthropic son pagos; Gemini tiene un plan gratuito suficiente para una demostración. Un asistente de compra para clientes tendría más impacto en la demo, pero expone la IA a usuarios anónimos (más superficie para prompt injection) y es más difícil de evaluar con métricas objetivas.

## Consecuencias
Cambiar de modelo es escribir otro adapter. El sistema funciona igual sin IA: con la IA apagada, caída o devolviendo algo inválido, el formulario sigue funcionando y la sugerencia sale de la plantilla. En el plan gratuito de Gemini, Google puede usar los datos enviados para mejorar sus productos: por eso solo se envían datos del producto (ver docs/ia/politica-de-datos.md). Los identificadores de modelo cambian seguido, por eso el modelo se configura con IA_MODELO. El simulado no mide la calidad del modelo real: el eval contra Gemini se corre aparte (ver evals/README.md).
