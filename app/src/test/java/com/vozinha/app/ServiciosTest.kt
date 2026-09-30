package com.vozinha.app

import com.vozinha.app.data.MedioComunicacion
import com.vozinha.app.data.TipoUsuario
import com.vozinha.app.data.Ubicacion
import com.vozinha.app.data.Usuario
import com.vozinha.app.data.UsuariosRepository
import com.vozinha.app.data.servicios.MensajesLocal
import com.vozinha.app.data.servicios.Resultado
import com.vozinha.app.data.servicios.UbicacionesLocal
import com.vozinha.app.data.servicios.UsuariosLocal
import com.vozinha.app.data.servicios.backendLocal
import com.vozinha.app.data.servicios.error
import com.vozinha.app.data.servicios.oBien
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas del CRUD de los servicios de datos.
 *
 * Trabajan sobre la implementación local, que respeta el mismo contrato que la
 * de Firebase. Verificar el contrato aquí permite probar toda la lógica sin
 * depender de la red ni de una cuenta del servicio.
 */
class ServiciosTest {

    private val correo = "camila@gmail.com"

    @Before
    fun dejarElArregloInicial() {
        UsuariosRepository.restaurar()
    }

    // ------------------------------- usuarios -------------------------------

    @Test
    fun `crea lee actualiza y elimina un usuario`() = runTest {
        val servicio = UsuariosLocal()
        val nuevo = Usuario(
            nombre = "Raúl Low",
            correo = "raul@gmail.com",
            password = "hola1234",
            tipoUsuario = TipoUsuario.PROFESIONAL,
            medioPreferido = MedioComunicacion.TEXTO_ESCRITO
        )

        assertTrue(servicio.crear(nuevo) is Resultado.Exito)
        assertEquals("Raúl Low", servicio.obtener("raul@gmail.com").oBien(null)?.nombre)

        servicio.actualizar(nuevo.copy(nombre = "Raúl Low Beattie"))
        assertEquals("Raúl Low Beattie", servicio.obtener("raul@gmail.com").oBien(null)?.nombre)

        assertTrue(servicio.eliminar("raul@gmail.com") is Resultado.Exito)
        assertNull(servicio.obtener("raul@gmail.com").oBien(null))
    }

    @Test
    fun `rechaza crear un usuario con un correo ya tomado`() = runTest {
        val servicio = UsuariosLocal()
        val repetido = Usuario(
            nombre = "Otra Camila",
            correo = "camila@gmail.com",
            password = "hola1234",
            tipoUsuario = TipoUsuario.FAMILIAR,
            medioPreferido = MedioComunicacion.TEXTO_ESCRITO
        )

        assertEquals("Ese correo ya tiene una cuenta", servicio.crear(repetido).error)
        assertEquals(5, servicio.listar().oBien(emptyList()).size)
    }

    @Test
    fun `no actualiza ni elimina un usuario inexistente`() = runTest {
        val servicio = UsuariosLocal()
        val fantasma = Usuario(
            nombre = "Nadie",
            correo = "nadie@gmail.com",
            password = "hola1234",
            tipoUsuario = TipoUsuario.FAMILIAR,
            medioPreferido = MedioComunicacion.TEXTO_ESCRITO
        )

        assertNotNull(servicio.actualizar(fantasma).error)
        assertNotNull(servicio.eliminar("nadie@gmail.com").error)
    }

    // ------------------------------- mensajes -------------------------------

    @Test
    fun `guarda un mensaje y lo devuelve en el listado`() = runTest {
        val servicio = MensajesLocal()

        val guardado = servicio.guardar(correo, "necesito ayuda")

        assertTrue(guardado is Resultado.Exito)
        assertEquals(listOf("necesito ayuda"), servicio.listar(correo).oBien(emptyList()).map { it.texto })
    }

    @Test
    fun `rechaza un mensaje vacio y uno repetido`() = runTest {
        val servicio = MensajesLocal()
        servicio.guardar(correo, "Necesito ayuda")

        assertNotNull(servicio.guardar(correo, "   ").error)
        assertNotNull(servicio.guardar(correo, "NECESITO AYUDA").error)
        assertEquals(1, servicio.listar(correo).oBien(emptyList()).size)
    }

    @Test
    fun `registrar la reproduccion suma una vez al contador`() = runTest {
        val servicio = MensajesLocal()
        val id = (servicio.guardar(correo, "Gracias") as Resultado.Exito).dato.id

        servicio.registrarReproduccion(correo, id)
        servicio.registrarReproduccion(correo, id)

        assertEquals(2, servicio.listar(correo).oBien(emptyList()).first().vecesDicho)
    }

    @Test
    fun `el favorito se enciende y se apaga`() = runTest {
        val servicio = MensajesLocal()
        val id = (servicio.guardar(correo, "Gracias") as Resultado.Exito).dato.id

        servicio.cambiarFavorito(correo, id)
        assertTrue(servicio.listar(correo).oBien(emptyList()).first().favorito)

        servicio.cambiarFavorito(correo, id)
        assertFalse(servicio.listar(correo).oBien(emptyList()).first().favorito)
    }

    @Test
    fun `elimina un mensaje y avisa cuando el identificador no existe`() = runTest {
        val servicio = MensajesLocal()
        val id = (servicio.guardar(correo, "Gracias") as Resultado.Exito).dato.id

        assertTrue(servicio.eliminar(correo, id) is Resultado.Exito)
        assertTrue(servicio.listar(correo).oBien(emptyList()).isEmpty())
        assertNotNull(servicio.eliminar(correo, "no-existe").error)
    }

    @Test
    fun `los mensajes de una persona no se mezclan con los de otra`() = runTest {
        val servicio = MensajesLocal()

        servicio.guardar("camila@gmail.com", "Mensaje de Camila")
        servicio.guardar("diego@gmail.com", "Mensaje de Diego")

        assertEquals(1, servicio.listar("camila@gmail.com").oBien(emptyList()).size)
        assertEquals("Mensaje de Diego", servicio.listar("diego@gmail.com").oBien(emptyList()).first().texto)
    }

    // ------------------------------ ubicaciones ------------------------------

    @Test
    fun `guarda ubicaciones y devuelve la mas reciente`() = runTest {
        val servicio = UbicacionesLocal()

        servicio.guardar(correo, Ubicacion(latitud = -33.44, longitud = -70.66, registradaEn = 100))
        servicio.guardar(correo, Ubicacion(latitud = -33.45, longitud = -70.67, registradaEn = 900))

        assertEquals(2, servicio.listar(correo).oBien(emptyList()).size)
        assertEquals(900L, servicio.ultima(correo).oBien(null)?.registradaEn)
    }

    @Test
    fun `borra el historial de ubicaciones`() = runTest {
        val servicio = UbicacionesLocal()
        servicio.guardar(correo, Ubicacion(registradaEn = 1))

        servicio.borrarHistorial(correo)

        assertTrue(servicio.listar(correo).oBien(emptyList()).isEmpty())
        assertNull(servicio.ultima(correo).oBien(null))
    }

    @Test
    fun `la ubicacion arma sus coordenadas y su enlace al mapa`() {
        val ubicacion = Ubicacion(latitud = -33.44890, longitud = -70.66930)

        assertEquals("-33.44890, -70.66930", ubicacion.coordenadas)
        assertTrue(ubicacion.enlaceMapa.startsWith("https://maps.google.com/?q="))
    }

    // ------------------------------ autenticación ----------------------------

    @Test
    fun `el backend local autentica contra el arreglo de usuarios`() = runTest {
        val backend = backendLocal()

        assertTrue(backend.autenticacion.iniciarSesion(correo, "hola1234") is Resultado.Exito)
        assertEquals(correo, backend.autenticacion.correoActual)

        backend.autenticacion.cerrarSesion()
        assertNull(backend.autenticacion.correoActual)
    }

    @Test
    fun `la recuperacion solo procede con un correo registrado`() = runTest {
        val backend = backendLocal()

        assertNull(backend.autenticacion.enviarCorreoDeRecuperacion(correo).error)
        assertNotNull(backend.autenticacion.enviarCorreoDeRecuperacion("nadie@gmail.com").error)
    }
}
