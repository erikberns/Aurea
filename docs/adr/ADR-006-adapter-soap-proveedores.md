# ADR-006: Adapter SOAP para reposición
Estado: implementada.

## Contexto
Aurea necesita solicitar mercadería a un proveedor legado. La aceptación remota y la recepción física son eventos diferentes. Un timeout no demuestra que el proveedor rechazó la solicitud.

## Decisión
Crear Reposición con IProveedorGateway e implementar un Adapter SOAP 1.1 document/literal. El proveedor vive en un proyecto y proceso separados, con WSDL y base H2 propios. Aurea registra la solicitud antes de contactar al proveedor y reutiliza su UUID para consultar o reintentar.

## Alternativas
Una llamada directa a clases Java del proveedor no demostraría integración externa. Compartir tablas acoplaría ambos sistemas. Crear siempre otra solicitud tras un timeout duplicaría pedidos.

## Consecuencias
El contrato está desacoplado del dominio. Hay latencia y fallos parciales; la UI muestra pendiente y permite reintentar. El Adapter valida XML y limita tiempos. El servicio de demostración escucha en loopback; TLS y autenticación del proveedor quedan fuera del despliegue local de este paso.
