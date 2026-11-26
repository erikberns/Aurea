# Política de datos del asistente de IA

Marco: Ley 25.326 de Protección de los Datos Personales (Argentina).

## Qué datos se envían al modelo
Solo datos del producto que carga el administrador: nombre de la pieza, material, detalles (piedras, terminación, estilo) y la lista de categorías del catálogo. **No se envían datos personales**: ni de clientes, ni de pedidos, ni del administrador. El formulario lo avisa y el validador descarta cualquier respuesta que contenga emails o teléfonos.

## Por qué importa
En el plan gratuito de Gemini, Google puede usar el contenido enviado para mejorar sus productos. Por eso la funcionalidad elegida trabaja solo con datos del catálogo, que además se publican en la tienda.

## Qué se guarda
Por cada llamada, en la tabla `ia_traza`: fecha, funcionalidad, versión del prompt, proveedor, modelo, resultado, latencia, tokens y costo estimado. **No se guarda** el texto enviado, el texto recibido ni quién hizo la solicitud. Los logs del backend registran lo mismo que la traza y nunca la clave de API.

## Clave de API
Se configura solo en el `.env` (IA_API_KEY), que está en `.gitignore`. Viaja únicamente en la cabecera `x-goog-api-key` hacia Google y nunca aparece en logs, errores ni respuestas.

## Retención
Las trazas se conservan mientras exista la base de datos de la demostración. Como no contienen datos personales, no requieren un plazo de borrado específico; si se quisiera, se pueden eliminar sin afectar el funcionamiento.

## Derechos de los titulares
Como no se tratan datos personales en esta funcionalidad, no hay datos que acceder, rectificar o suprimir.
