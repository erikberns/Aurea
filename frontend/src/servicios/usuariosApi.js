const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

async function solicitar(ruta, opciones = {}) {
  const respuesta = await fetch(`${API_URL}${ruta}`, {
    ...opciones,
    headers: {
      'Content-Type': 'application/json',
      ...opciones.headers,
    },
  })

  if (!respuesta.ok) {
    const error = await respuesta.json().catch(() => null)
    throw new Error(error?.mensaje ?? 'No se pudo completar la operacion')
  }

  return respuesta.json()
}

export function registrarCliente(datos) {
  return solicitar('/api/usuarios', {
    method: 'POST',
    body: JSON.stringify(datos),
  })
}

export function autenticarUsuario(credenciales) {
  return solicitar('/api/usuarios/autenticar', {
    method: 'POST',
    body: JSON.stringify(credenciales),
  })
}

export function consultarPerfil(usuarioId) {
  return solicitar(`/api/usuarios/${usuarioId}`)
}

export function actualizarPerfil(usuarioId, datos) {
  return solicitar(`/api/usuarios/${usuarioId}`, {
    method: 'PATCH',
    body: JSON.stringify(datos),
  })
}
