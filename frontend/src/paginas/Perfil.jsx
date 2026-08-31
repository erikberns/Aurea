import { useState } from 'react'
import { actualizarPerfil, consultarPerfil } from '../servicios/usuariosApi.js'

function Perfil({ usuarioInicial, onUsuarioCargado }) {
  const [usuario, setUsuario] = useState(usuarioInicial)
  const [usuarioId, setUsuarioId] = useState(usuarioInicial?.id ?? '')
  const [nombre, setNombre] = useState(usuarioInicial?.nombre ?? '')
  const [apellido, setApellido] = useState(usuarioInicial?.apellido ?? '')
  const [mensaje, setMensaje] = useState('')
  const [error, setError] = useState('')

  async function buscar(evento) {
    evento.preventDefault()
    setError('')
    setMensaje('')

    try {
      const encontrado = await consultarPerfil(usuarioId)
      setUsuario(encontrado)
      setNombre(encontrado.nombre)
      setApellido(encontrado.apellido)
      onUsuarioCargado(encontrado)
    } catch (errorApi) {
      setError(errorApi.message)
    }
  }

  async function guardar(evento) {
    evento.preventDefault()
    setError('')
    setMensaje('')

    try {
      const actualizado = await actualizarPerfil(usuario.id, { nombre, apellido })
      setUsuario(actualizado)
      onUsuarioCargado(actualizado)
      setMensaje('Perfil actualizado.')
    } catch (errorApi) {
      setError(errorApi.message)
    }
  }

  return (
    <section>
      <h2>Perfil</h2>

      {!usuario && (
        <form onSubmit={buscar}>
          <label>
            ID del usuario
            <input
              type="number"
              min="1"
              value={usuarioId}
              onChange={(evento) => setUsuarioId(evento.target.value)}
              required
            />
          </label>
          <button type="submit">Consultar perfil</button>
        </form>
      )}

      {usuario && (
        <>
          <div className="datos-perfil">
            <p><strong>ID:</strong> {usuario.id}</p>
            <p><strong>Email:</strong> {usuario.email}</p>
            <p><strong>Rol:</strong> {usuario.rol}</p>
          </div>

          <form onSubmit={guardar}>
            <label>
              Nombre
              <input value={nombre} onChange={(evento) => setNombre(evento.target.value)} required />
            </label>
            <label>
              Apellido
              <input value={apellido} onChange={(evento) => setApellido(evento.target.value)} required />
            </label>
            <button type="submit">Guardar cambios</button>
          </form>
        </>
      )}

      {mensaje && <p className="mensaje-exito">{mensaje}</p>}
      {error && <p className="mensaje-error">{error}</p>}
    </section>
  )
}

export default Perfil
