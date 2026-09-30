package com.vozinha.app

import com.vozinha.app.data.FRASES_INICIALES
import com.vozinha.app.data.MedioComunicacion
import com.vozinha.app.data.TipoUsuario
import com.vozinha.app.data.Usuario
import com.vozinha.app.data.UsuariosRepository
import com.vozinha.app.ui.AppViewModel
import com.vozinha.app.ui.ResultadoRegistro
import com.vozinha.app.util.Validaciones

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas de las validaciones, del arreglo de usuarios y de la sesión.
 */
class VozinhaTest {

    @Before
    fun prepararArreglo() {
        UsuariosRepository.restaurar()
    }

    private fun usuarioNuevo(correo: String = "nuevo@gmail.com") = Usuario(
        nombre = "Persona Nueva",
        correo = correo,
        password = "clave123",
        tipoUsuario = TipoUsuario.PERSONA_SORDA,
        medioPreferido = MedioComunicacion.TEXTO_ESCRITO
    )

    @Test
    fun `valida el formato del correo`() {
        assertTrue(Validaciones.esEmailValido("camila@gmail.com"))
        assertFalse(Validaciones.esEmailValido("camila"))
        assertFalse(Validaciones.esEmailValido("camila@correo"))
        assertFalse(Validaciones.esEmailValido(""))
    }

    @Test
    fun `la contrasena exige el largo minimo`() {
        assertFalse(Validaciones.esPasswordValida("12345"))
        assertTrue(Validaciones.esPasswordValida("123456"))
    }

    @Test
    fun `las contrasenas deben coincidir y no estar vacias`() {
        assertTrue(Validaciones.coincidenPasswords("hola1234", "hola1234"))
        assertFalse(Validaciones.coincidenPasswords("hola1234", "otra"))
        assertFalse(Validaciones.coincidenPasswords("", ""))
    }

    @Test
    fun `los mensajes de error orientan al usuario`() {
        assertEquals("Escribe tu correo electrónico", Validaciones.errorEmail(""))
        assertEquals("El correo debe incluir el símbolo @", Validaciones.errorEmail("camila"))
        assertNull(Validaciones.errorEmail("camila@gmail.com"))
        assertEquals(
            "Las dos contraseñas no son iguales",
            Validaciones.errorConfirmacion("hola1234", "otra")
        )
    }

    @Test
    fun `el arreglo parte con cinco usuarios validos`() {
        val usuarios = UsuariosRepository.obtenerUsuarios()
        assertEquals(UsuariosRepository.USUARIOS_INICIALES, usuarios.size)
        usuarios.forEach {
            assertTrue(it.correo.contains("@"))
            assertTrue(it.password.length >= Validaciones.LARGO_MINIMO_PASSWORD)
        }
        assertEquals(usuarios.size, usuarios.map { it.correo.lowercase() }.distinct().size)
    }

    @Test
    fun `registrar agrega el usuario al arreglo`() {
        assertTrue(UsuariosRepository.agregarUsuario(usuarioNuevo()))
        assertEquals(6, UsuariosRepository.obtenerUsuarios().size)
    }

    @Test
    fun `no permite registrar dos veces el mismo correo`() {
        assertTrue(UsuariosRepository.agregarUsuario(usuarioNuevo()))
        assertFalse(UsuariosRepository.agregarUsuario(usuarioNuevo()))
        assertEquals(6, UsuariosRepository.obtenerUsuarios().size)
    }

    @Test
    fun `el correo repetido se detecta sin distinguir mayusculas`() {
        assertTrue(UsuariosRepository.existeCorreo("CAMILA@GMAIL.COM"))
        assertFalse(UsuariosRepository.existeCorreo("desconocido@gmail.com"))
    }

    @Test
    fun `el tipo de usuario y el medio son valores controlados`() {
        UsuariosRepository.obtenerUsuarios().forEach { usuario ->
            assertTrue(usuario.tipoUsuario in TipoUsuario.entries)
            assertTrue(usuario.medioPreferido in MedioComunicacion.entries)
            assertTrue(usuario.tipoUsuario.etiqueta.isNotBlank())
        }
    }

    @Test
    fun `valida el acceso contra el arreglo`() {
        assertNotNull(UsuariosRepository.validarAcceso("camila@gmail.com", "hola1234"))
        assertNull(UsuariosRepository.validarAcceso("camila@gmail.com", "incorrecta"))
        assertNull(UsuariosRepository.validarAcceso("nadie@gmail.com", "hola1234"))
    }

    @Test
    fun `actualiza la ficha de un usuario conservando su correo`() {
        val camila = UsuariosRepository.buscarPorCorreo("camila@gmail.com")!!

        val cambiado = UsuariosRepository.reemplazar(
            correo = "camila@gmail.com",
            usuario = camila.copy(nombre = "Camila Rojas Díaz")
        )

        assertTrue(cambiado)
        assertEquals("Camila Rojas Díaz", UsuariosRepository.buscarPorCorreo("camila@gmail.com")?.nombre)
        assertEquals(5, UsuariosRepository.obtenerUsuarios().size)
    }

    @Test
    fun `elimina un usuario del arreglo`() {
        assertTrue(UsuariosRepository.eliminarUsuario("JOSEFA@gmail.com"))

        assertNull(UsuariosRepository.buscarPorCorreo("josefa@gmail.com"))
        assertEquals(4, UsuariosRepository.obtenerUsuarios().size)
    }

    @Test
    fun `no elimina un usuario que no existe`() {
        assertFalse(UsuariosRepository.eliminarUsuario("nadie@gmail.com"))
        assertEquals(5, UsuariosRepository.obtenerUsuarios().size)
    }
}
