function Home({ onRegistrar }) {
  return (
    <section>
      <h2>Inicio</h2>
      <p>Demostracion del componente ServicioDeUsuarios.</p>
      <p>Desde esta interfaz puede registrar un cliente y consultar o modificar su perfil.</p>
      <button type="button" onClick={onRegistrar}>Crear cuenta</button>
    </section>
  )
}

export default Home
