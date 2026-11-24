# Proveedor SOAP externo de demostración

Esta aplicación representa el sistema legado de un proveedor de joyería. Se versiona dentro del repositorio de Aurea para poder levantar todo con un solo comando, pero no forma parte del código principal: es un proyecto aparte, con su propio build, proceso y contenedor. Implementa un servicio SOAP 1.1 real mediante Eclipse Metro.

## Arranque

Requiere Java 25 y Maven. Desde esta carpeta:

```powershell
mvn test
mvn package
java -jar target/proveedor-soap-1.0.0.jar
```

Endpoint: http://127.0.0.1:8091/soap/proveedor

WSDL activo: http://127.0.0.1:8091/soap/proveedor?wsdl

Copias exportadas del servicio: [WSDL](contratos/proveedor.wsdl) y [XSD](contratos/proveedor.xsd). El WSDL exportado referencia el XSD local y se puede importar en SoapUI.

Namespace: `urn:aurea:proveedor:v1`. Operaciones: `consultarDisponibilidad(codigoProducto)`, `solicitarReposicion(idSolicitud, codigoProducto, cantidad)` y `consultarEstado(idSolicitud)`.

El catálogo inicial tiene ANILLO-PLATA, COLLAR-ORO y PULSERA-PLATA, con 1.000 unidades cada uno. Se crea únicamente si no existe. La cantidad solicitada debe estar entre 1 y 10.000.

La base H2 propia se guarda en `data/`. Solicitudes y stock sobreviven a reinicios. Repetir el mismo UUID y datos devuelve el resultado anterior, sin volver a descontar stock. Reutilizar el UUID con otros datos devuelve SOAP Fault. No hay aceptación parcial.

Variables opcionales: PROVEEDOR_ENDPOINT, PROVEEDOR_DB_URL y PROVEEDOR_DB_PASSWORD. Para pruebas aisladas se utiliza el puerto 18091 y una base en memoria. Por defecto el servicio escucha en loopback; dentro de Docker Compose escucha en `0.0.0.0` para que el backend lo alcance por la red interna. no tiene autenticación ni TLS y está destinado a demostración local.

## Límites entre aplicaciones

- `../aurea-backend/` y `../aurea-frontend/`: aplicación principal. El componente de Reposición y el cliente SOAP pertenecen a Aurea.
- `proveedor-soap/`: servicio SOAP externo, WSDL y datos propios del proveedor.
- La comunicación entre aplicaciones es exclusivamente SOAP sobre HTTP, con la dirección del proveedor configurable en Aurea.
- El proveedor tiene su propio proyecto de compilación, configuración, puerto y proceso de ejecución. No es un módulo Maven de Aurea.
- No comparte entidades JPA, repositorios, tablas ni código de negocio con Aurea. El Adapter de Aurea consume el contrato XML sin importar clases de este proyecto.

## Alcance implementado

Consultar disponibilidad, solicitar reposición con un identificador que evite duplicados y consultar el estado de una solicitud. No requiere frontend.

El proveedor es una simulación académica de un sistema externo con comunicación SOAP real. Su código no debe contabilizarse como un componente interno de Aurea. Aceptar una solicitud no equivale a recibir físicamente mercadería; esa recepción se registra luego en Aurea.

Las cuatro pruebas de ProveedorSoapTests comprueban WSDL, requests SOAP sobre HTTP, idempotencia, SOAP Fault y rechazo por falta de stock. Referencia: [Eclipse Metro](https://eclipse-ee4j.github.io/metro-jax-ws/).

## Versionado

Esta carpeta se versiona dentro del repositorio de Aurea. En `docker-compose.yml` de la raíz se construye con su propio `Dockerfile` y corre en su propio contenedor, con un volumen para sus datos.
