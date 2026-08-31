# Frontend de Aurea

Interfaz React minima para demostrar el componente `ServicioDeUsuarios`.

## Funcionalidades

- Inicio.
- Registro de un cliente mediante `POST /api/usuarios`.
- Inicio de sesion mediante `POST /api/usuarios/autenticar`.
- Consulta de perfil mediante `GET /api/usuarios/{id}`.
- Actualizacion de nombre y apellido mediante `PATCH /api/usuarios/{id}`.
- Cierre de sesion local mediante el boton `Salir`.

La interfaz no implementa reglas de negocio ni accede a PostgreSQL. Toda operacion se realiza mediante la API REST de Spring Boot, respetando la separacion entre presentacion, negocio y datos.

## Ejecucion

Con el backend ejecutandose en `http://localhost:8080`:

```powershell
npm install
npm run dev
```

Luego abrir `http://localhost:5173`.

Para usar otra URL de backend, copiar `.env.example` como `.env` y modificar `VITE_API_URL`.
