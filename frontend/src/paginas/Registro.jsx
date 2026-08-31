import { useState } from 'react'
import { registrarCliente } from '../servicios/usuariosApi.js'

const formularioInicial = {
  nombre: '',
  apellido: '',
  email: '',
  contrasena: '',
}

function Registro({ onRegistrado }) {
  const [formulario, setFormulario] = useState(formularioInicial)
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  function actualizarCampo(evento) {
    const { name, value } = evento.target
    setFormulario((actual) => ({ ...actual, [name]: value }))
  }

  async function enviar(evento) {
    evento.preventDefault()
    setError('')
    setEnviando(true)

    try {
      const usuario = await registrarCliente(formulario)
      setFormulario(formularioInicial)
      onRegistrado(usuario)
    } catch (errorApi) {
      setError(errorApi.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section>
      <h2>Registro</h2>
      <form onSubmit={enviar}>
        <label>
          Nombre
          <input name="nombre" value={formulario.nombre} onChange={actualizarCampo} required />
        </label>
        <label>
          Apellido
          <input name="apellido" value={formulario.apellido} onChange={actualizarCampo} required />
        </label>
        <label>
          Email
          <input type="email" name="email" value={formulario.email} onChange={actualizarCampo} required />
        </label>
        <label>
          Contrasena
          <input
            type="password"
            name="contrasena"
            minLength="8"
            value={formulario.contrasena}
            onChange={actualizarCampo}
            required
          />
        </label>
        <button type="submit" disabled={enviando}>
          {enviando ? 'Registrando...' : 'Registrar cliente'}
        </button>
      </form>
      {error && <p className="mensaje-error">{error}</p>}
    </section>
  )
}

export default Registro
