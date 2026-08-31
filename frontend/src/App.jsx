import { useState } from 'react'
import Home from './paginas/Home.jsx'
import InicioSesion from './paginas/InicioSesion.jsx'
import Perfil from './paginas/Perfil.jsx'
import Registro from './paginas/Registro.jsx'
import './App.css'

function App() {
  const [pagina, setPagina] = useState('home')
  const [usuarioActual, setUsuarioActual] = useState(null)

  function mostrarPerfil(usuario) {
    setUsuarioActual(usuario)
    setPagina('perfil')
  }

  function cerrarSesion() {
    setUsuarioActual(null)
    setPagina('home')
  }

  return (
    <div className="aplicacion">
      <header>
        <h1>Aurea</h1>
        <nav aria-label="Navegacion principal">
          <button type="button" onClick={() => setPagina('home')}>Inicio</button>
          {!usuarioActual && (
            <>
              <button type="button" onClick={() => setPagina('login')}>Iniciar sesion</button>
              <button type="button" onClick={() => setPagina('registro')}>Registro</button>
            </>
          )}
          {usuarioActual && (
            <>
              <button type="button" onClick={() => setPagina('perfil')}>Perfil</button>
              <button type="button" onClick={cerrarSesion}>Salir</button>
            </>
          )}
        </nav>
      </header>

      <main>
        {pagina === 'home' && <Home onRegistrar={() => setPagina('registro')} />}
        {pagina === 'login' && <InicioSesion onAutenticado={mostrarPerfil} />}
        {pagina === 'registro' && <Registro onRegistrado={mostrarPerfil} />}
        {pagina === 'perfil' && (
          <Perfil usuarioInicial={usuarioActual} onUsuarioCargado={setUsuarioActual} />
        )}
      </main>
    </div>
  )
}

export default App
