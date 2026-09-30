package com.vozinha.app

import com.vozinha.app.data.MedioComunicacion
import com.vozinha.app.data.Mensaje
import com.vozinha.app.data.TipoUsuario
import com.vozinha.app.data.Usuario
import com.vozinha.app.data.servicios.Backend
import com.vozinha.app.data.servicios.Resultado
import com.vozinha.app.data.servicios.ServicioAutenticacion
import com.vozinha.app.data.servicios.ServicioMensajes
import com.vozinha.app.data.servicios.ServicioUbicaciones
import com.vozinha.app.data.servicios.ServicioUsuarios
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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

/**
 * Pruebas con dobles de Mockito.
 *
 * Las pruebas anteriores comprueban qué hace la aplicación. Estas comprueban
 * cómo lo hace: que el ViewModel llame al servicio correcto, con los datos
 * correctos, y que no siga adelante cuando el servicio rechaza la operación.
 *
 * Como los servicios son interfaces, el doble las reemplaza sin necesidad de
 * red ni de una cuenta de Firebase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MockitoTest {

    private val despachador = StandardTestDispatcher()

    private lateinit var autenticacion: ServicioAutenticacion
    private lateinit var usuarios: ServicioUsuarios
    private lateinit var mensajes: ServicioMensajes
    private lateinit var ubicaciones: ServicioUbicaciones

    private val camila = Usuario(
        nombre = "Camila Rojas",
        correo = "camila@gmail.com",
        password = "hola1234",
        tipoUsuario = TipoUsuario.PERSONA_SORDA,
        medioPreferido = MedioComunicacion.LENGUA_DE_SENAS
    )

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
        autenticacion = mock()
        usuarios = mock()
        mensajes = mock()
        ubicaciones = mock()
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AppViewModel(
        Backend(autenticacion, usuarios, mensajes, ubicaciones, "Doble de prueba")
    )

    @Test
    fun `el inicio de sesion consulta la autenticacion y luego la ficha`() = runTest {
        autenticacion.stub {
            onBlocking { iniciarSesion("camila@gmail.com", "hola1234") }
                .thenReturn(Resultado.Exito("camila@gmail.com"))
        }
        usuarios.stub {
            onBlocking { obtener("camila@gmail.com") }.thenReturn(Resultado.Exito(camila))
        }
        mensajes.stub { onBlocking { listar(any()) }.thenReturn(Resultado.Exito(emptyList())) }
        ubicaciones.stub { onBlocking { listar(any()) }.thenReturn(Resultado.Exito(emptyList())) }

        val viewModel = viewModel()
        var error: String? = "sin respuesta"
        viewModel.iniciarSesion("camila@gmail.com", "hola1234") { error = it }
        advanceUntilIdle()

        verify(autenticacion).iniciarSesion("camila@gmail.com", "hola1234")
        verify(usuarios).obtener("camila@gmail.com")
        assertNull(error)
        assertEquals(camila, viewModel.sesionActiva)
    }

    @Test
    fun `si la autenticacion falla no se consulta la ficha del usuario`() = runTest {
        autenticacion.stub {
            onBlocking { iniciarSesion(any(), any()) }
                .thenReturn(Resultado.Fallo("Credenciales incorrectas"))
        }

        val viewModel = viewModel()
        var error: String? = null
        viewModel.iniciarSesion("camila@gmail.com", "mala") { error = it }
        advanceUntilIdle()

        assertEquals("Credenciales incorrectas", error)
        verifyNoInteractions(usuarios)
        assertNull(viewModel.sesionActiva)
    }

    @Test
    fun `el registro crea la cuenta y despues la ficha`() = runTest {
        autenticacion.stub {
            onBlocking { registrar(any(), any()) }.thenReturn(Resultado.Exito("raul@gmail.com"))
        }
        usuarios.stub {
            onBlocking { crear(any()) }.thenReturn(Resultado.Exito(Unit))
            onBlocking { listar() }.thenReturn(Resultado.Exito(listOf(camila)))
        }

        val nuevo = camila.copy(nombre = "Raúl Low", correo = "raul@gmail.com")
        var resultado: ResultadoRegistro? = null
        viewModel().registrar(nuevo) { resultado = it }
        advanceUntilIdle()

        verify(autenticacion).registrar("raul@gmail.com", "hola1234")
        verify(usuarios).crear(nuevo)
        assertTrue(resultado is ResultadoRegistro.Exitoso)
    }

    @Test
    fun `si la cuenta no se crea no se guarda la ficha`() = runTest {
        autenticacion.stub {
            onBlocking { registrar(any(), any()) }
                .thenReturn(Resultado.Fallo("Ese correo ya tiene una cuenta"))
        }

        var resultado: ResultadoRegistro? = null
        viewModel().registrar(camila) { resultado = it }
        advanceUntilIdle()

        assertTrue(resultado is ResultadoRegistro.Rechazado)
        verify(usuarios, never()).crear(any())
    }

    @Test
    fun `guardar un mensaje lo normaliza antes de enviarlo al servicio`() = runTest {
        prepararSesion()
        mensajes.stub {
            onBlocking { guardar(any(), any()) }
                .thenReturn(Resultado.Exito(Mensaje("1", "Necesito ayuda", 0L)))
        }

        val viewModel = viewModel()
        viewModel.iniciarSesion("camila@gmail.com", "hola1234") {}
        advanceUntilIdle()

        viewModel.guardarMensaje("  necesito   ayuda ") {}
        advanceUntilIdle()

        // El texto llega al servicio ya parejo, no como lo escribió la persona.
        verify(mensajes).guardar(eq("camila@gmail.com"), eq("Necesito ayuda"))
    }

    @Test
    fun `el error del servicio de mensajes llega a la pantalla`() = runTest {
        prepararSesion()
        mensajes.stub {
            onBlocking { guardar(any(), any()) }
                .thenReturn(Resultado.Fallo("Ese mensaje ya está guardado"))
        }

        val viewModel = viewModel()
        viewModel.iniciarSesion("camila@gmail.com", "hola1234") {}
        advanceUntilIdle()

        var error: String? = null
        viewModel.guardarMensaje("Gracias") { error = it }
        advanceUntilIdle()

        assertEquals("Ese mensaje ya está guardado", error)
    }

    @Test
    fun `cerrar sesion avisa al servicio de autenticacion`() = runTest {
        prepararSesion()
        val viewModel = viewModel()
        viewModel.iniciarSesion("camila@gmail.com", "hola1234") {}
        advanceUntilIdle()

        viewModel.cerrarSesion()

        verify(autenticacion).cerrarSesion()
        assertNull(viewModel.sesionActiva)
    }

    @Test
    fun `la recuperacion delega en el servicio de autenticacion`() = runTest {
        autenticacion.stub {
            onBlocking { enviarCorreoDeRecuperacion(any()) }
                .thenReturn(Resultado.Fallo("No encontramos una cuenta con ese correo"))
        }

        var error: String? = null
        viewModel().recuperarPassword("nadie@gmail.com") { error = it }
        advanceUntilIdle()

        verify(autenticacion).enviarCorreoDeRecuperacion("nadie@gmail.com")
        assertNotNull(error)
    }

    /** Deja los dobles listos para que el inicio de sesión llegue hasta el final. */
    private fun prepararSesion() {
        autenticacion.stub {
            onBlocking { iniciarSesion(any(), any()) }.thenReturn(Resultado.Exito("camila@gmail.com"))
        }
        usuarios.stub { onBlocking { obtener(any()) }.thenReturn(Resultado.Exito(camila)) }
        mensajes.stub { onBlocking { listar(any()) }.thenReturn(Resultado.Exito(emptyList())) }
        ubicaciones.stub { onBlocking { listar(any()) }.thenReturn(Resultado.Exito(emptyList())) }
    }
}
