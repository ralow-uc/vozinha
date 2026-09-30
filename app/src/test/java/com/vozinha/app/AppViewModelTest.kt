package com.vozinha.app

import com.vozinha.app.data.MedioComunicacion
import com.vozinha.app.data.TipoUsuario
import com.vozinha.app.data.Ubicacion
import com.vozinha.app.data.Usuario
import com.vozinha.app.data.UsuariosRepository
import com.vozinha.app.data.servicios.Resultado
import com.vozinha.app.data.servicios.backendLocal
import com.vozinha.app.ui.AppViewModel
import com.vozinha.app.ui.ResultadoRegistro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas del ViewModel sobre el backend local.
 *
 * El ViewModel lanza su trabajo en viewModelScope, que se apoya en el
 * despachador principal de Android. En una prueba de JVM ese despachador no
 * existe, así que se reemplaza por uno de prueba que además permite decidir
 * cuándo avanza el tiempo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
        UsuariosRepository.restaurar()
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun usuarioNuevo(correo: String) = Usuario(
        nombre = "Raúl Low",
        correo = correo,
        password = "hola1234",
        tipoUsuario = TipoUsuario.PROFESIONAL,
        medioPreferido = MedioComunicacion.TEXTO_ESCRITO
    )

    @Test
    fun `inicia sesion con un usuario del arreglo`() = runTest {
        val viewModel = AppViewModel(backendLocal())
        var error: String? = "sin respuesta"

        viewModel.iniciarSesion("camila@gmail.com", "hola1234") { error = it }
        advanceUntilIdle()

        assertNull(error)
        assertEquals("Camila Rojas", viewModel.sesionActiva?.nombre)
    }

    @Test
    fun `rechaza credenciales incorrectas y no abre sesion`() = runTest {
        val viewModel = AppViewModel(backendLocal())
        var error: String? = null

        viewModel.iniciarSesion("camila@gmail.com", "otra clave") { error = it }
        advanceUntilIdle()

        assertNotNull(error)
        assertNull(viewModel.sesionActiva)
    }

    @Test
    fun `el usuario recien registrado puede ingresar`() = runTest {
        val viewModel = AppViewModel(backendLocal())
        var registro: ResultadoRegistro? = null

        viewModel.registrar(usuarioNuevo("recien@gmail.com")) { registro = it }
        advanceUntilIdle()

        assertTrue(registro is ResultadoRegistro.Exitoso)
        assertEquals(6, viewModel.totalUsuarios)

        var error: String? = "sin respuesta"
        viewModel.iniciarSesion("recien@gmail.com", "hola1234") { error = it }
        advanceUntilIdle()
        assertNull(error)
    }

    @Test
    fun `avisa cuando el correo ya esta registrado`() = runTest {
        val viewModel = AppViewModel(backendLocal())
        var registro: ResultadoRegistro? = null

        viewModel.registrar(usuarioNuevo("camila@gmail.com")) { registro = it }
        advanceUntilIdle()

        assertTrue(registro is ResultadoRegistro.Rechazado)
        assertEquals(5, viewModel.totalUsuarios)
    }

    @Test
    fun `guarda un mensaje y lo deja disponible para la view Hablar`() = runTest {
        val viewModel = sesionAbierta()

        viewModel.guardarMensaje("necesito un intérprete") {}
        advanceUntilIdle()

        assertEquals(1, viewModel.mensajes.size)
        assertEquals("Necesito un intérprete", viewModel.mensajes.first().texto)
    }

    @Test
    fun `registrar la reproduccion actualiza el contador del mensaje`() = runTest {
        val viewModel = sesionAbierta()
        viewModel.guardarMensaje("Gracias") {}
        advanceUntilIdle()

        viewModel.registrarReproduccion(viewModel.mensajes.first().id)
        advanceUntilIdle()

        assertEquals(1, viewModel.mensajes.first().vecesDicho)
    }

    @Test
    fun `elimina un mensaje guardado`() = runTest {
        val viewModel = sesionAbierta()
        viewModel.guardarMensaje("Gracias") {}
        advanceUntilIdle()

        viewModel.eliminarMensaje(viewModel.mensajes.first().id)
        advanceUntilIdle()

        assertTrue(viewModel.mensajes.isEmpty())
    }

    @Test
    fun `guarda la ubicacion y la deja en el historial`() = runTest {
        val viewModel = sesionAbierta()

        viewModel.guardarUbicacion(
            Ubicacion(latitud = -33.44, longitud = -70.66, registradaEn = 100)
        ) {}
        advanceUntilIdle()

        assertEquals(1, viewModel.ubicaciones.size)

        viewModel.borrarHistorialDeUbicaciones()
        advanceUntilIdle()
        assertTrue(viewModel.ubicaciones.isEmpty())
    }

    @Test
    fun `cerrar sesion olvida la cuenta y sus datos`() = runTest {
        val viewModel = sesionAbierta()
        viewModel.guardarMensaje("Gracias") {}
        advanceUntilIdle()

        viewModel.cerrarSesion()

        assertNull(viewModel.sesionActiva)
        assertTrue(viewModel.mensajes.isEmpty())
        assertTrue(viewModel.ubicaciones.isEmpty())
    }

    @Test
    fun `la recuperacion de contrasena solo procede con un correo registrado`() = runTest {
        val viewModel = AppViewModel(backendLocal())
        var error: String? = "sin respuesta"

        viewModel.recuperarPassword("camila@gmail.com") { error = it }
        advanceUntilIdle()
        assertNull(error)

        viewModel.recuperarPassword("nadie@gmail.com") { error = it }
        advanceUntilIdle()
        assertNotNull(error)
    }

    /** Devuelve un ViewModel con la sesión de Camila ya abierta. */
    private suspend fun kotlinx.coroutines.test.TestScope.sesionAbierta(): AppViewModel {
        val viewModel = AppViewModel(backendLocal())
        viewModel.iniciarSesion("camila@gmail.com", "hola1234") {}
        advanceUntilIdle()
        return viewModel
    }
}
