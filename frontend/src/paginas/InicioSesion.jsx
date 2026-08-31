import { useState } from 'react'
import { autenticarUsuario } from '../servicios/usuariosApi.js'

function InicioSesion({ onAutenticado }) {
  const [email, setEmail] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento) {
    evento.preventDefault()
    setError('')
    setEnviando(true)

    try {
      const usuario = await autenticarUsuario({ email, contrasena })
      onAutenticado(usuario)
    } catch (errorApi) {
      setError(errorApi.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section>
      <h2>Iniciar sesion</h2>
      <form onSubmit={enviar}>
        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(evento) => setEmail(evento.target.value)}
            required
          />
        </label>
        <label>
          Contrasena
          <input
            type="password"
            value={contrasena}
            onChange={(evento) => setContrasena(evento.target.value)}
            required
          />
        </label>
        <button type="submit" disabled={enviando}>
          {enviando ? 'Ingresando...' : 'Ingresar'}
        </button>
      </form>
      {error && <p className="mensaje-error">{error}</p>}
    </section>
  )
}

export default InicioSesion
