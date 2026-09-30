package com.vozinha.app

import com.vozinha.app.data.MedioComunicacion
import com.vozinha.app.data.TipoUsuario
import com.vozinha.app.data.Usuario
import com.vozinha.app.data.UsuariosRepository
import com.vozinha.app.util.ExigenciaCorreo
import com.vozinha.app.util.comoFrase
import com.vozinha.app.util.conCorreo
import com.vozinha.app.util.esCorreo
import com.vozinha.app.util.intentar
import com.vozinha.app.util.mensajeLegible
import com.vozinha.app.util.normalizado
import com.vozinha.app.util.primerError
import com.vozinha.app.util.reglaDeCorreo
import com.vozinha.app.util.resumen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas de las funciones de extensión, las funciones de orden superior y el
 * manejo de errores que incorpora esta entrega.
 */
class ExtensionesTest {

    @Before
    fun dejarElArregloInicial() {
        UsuariosRepository.restaurar()
    }

    // ----------------------- Funciones de extensión -----------------------

    @Test
    fun `normalizado quita los espacios sobrantes`() {
        assertEquals("necesito ayuda", "  necesito   ayuda  ".normalizado())
        assertEquals("", "     ".normalizado())
    }

    @Test
    fun `la propiedad de extension reconoce un correo`() {
        assertTrue("camila@gmail.com".esCorreo)
        assertFalse("camila".esCorreo)
        assertFalse("".esCorreo)
    }

    @Test
    fun `comoFrase deja la frase pareja`() {
        assertEquals("Necesito ayuda", "  necesito   ayuda ".comoFrase())
        assertEquals("Gracias", "gracias".comoFrase())
    }

    @Test
    fun `conCorreo busca sin distinguir mayusculas ni espacios`() {
        val usuarios = UsuariosRepository.obtenerUsuarios()

        assertEquals("Camila Rojas", usuarios.conCorreo("  CAMILA@Gmail.com ")?.nombre)
        assertNull(usuarios.conCorreo("nadie@gmail.com"))
    }

    @Test
    fun `la propiedad resumen describe al usuario`() {
        val camila = UsuariosRepository.obtenerUsuarios().first()

        assertEquals("Camila Rojas, persona sorda", camila.resumen)
    }

    // -------------------- Funciones de orden superior --------------------

    @Test
    fun `la regla de solo formato acepta cualquier correo bien escrito`() {
        val regla = reglaDeCorreo(ExigenciaCorreo.SOLO_FORMATO)

        assertNull(regla("nadie@gmail.com"))
        assertNotNull(regla("nadie"))
    }

    @Test
    fun `la regla del registro rechaza un correo ya tomado`() {
        val regla = reglaDeCorreo(ExigenciaCorreo.DEBE_SER_NUEVO, UsuariosRepository::existeCorreo)

        assertEquals("Ese correo ya tiene una cuenta", regla("camila@gmail.com"))
        assertNull(regla("nuevo@gmail.com"))
    }

    @Test
    fun `la regla de recuperacion exige que el correo exista`() {
        val regla = reglaDeCorreo(ExigenciaCorreo.DEBE_EXISTIR, UsuariosRepository::existeCorreo)

        assertNull(regla("diego@gmail.com"))
        assertEquals("No encontramos una cuenta con ese correo", regla("nadie@gmail.com"))
    }

    @Test
    fun `el formato se revisa antes que la existencia del correo`() {
        val regla = reglaDeCorreo(ExigenciaCorreo.DEBE_EXISTIR, UsuariosRepository::existeCorreo)

        assertEquals("El correo debe incluir el símbolo @", regla("camila"))
    }

    @Test
    fun `primerError devuelve la primera regla que falla`() {
        val error = primerError(
            { null },
            { "Falta el nombre" },
            { "Falta el correo" }
        )

        assertEquals("Falta el nombre", error)
    }

    @Test
    fun `primerError devuelve null cuando todas las reglas pasan`() {
        assertNull(primerError({ null }, { null }))
    }

    @Test
    fun `primerError no evalua las reglas siguientes`() {
        var evaluadas = 0

        primerError(
            { evaluadas++; null },
            { evaluadas++; "Error" },
            { evaluadas++; "Nunca se llega aquí" }
        )

        assertEquals(2, evaluadas)
    }

    // -------------------------- Manejo de errores --------------------------

    @Test
    fun `intentar devuelve el valor cuando no hay excepcion`() {
        val resultado = intentar { 2 + 2 }

        assertEquals(4, resultado)
    }

    @Test
    fun `intentar captura la excepcion y avisa en lugar de propagarla`() {
        var capturado: String? = null

        val resultado = intentar(alFallar = { error -> capturado = error.mensajeLegible() }) {
            throw IllegalStateException("El motor de voz no está disponible")
        }

        assertNull(resultado)
        assertEquals("El motor de voz no está disponible", capturado)
    }

    @Test
    fun `mensajeLegible usa el nombre de la excepcion cuando no hay mensaje`() {
        assertEquals("IllegalArgumentException", IllegalArgumentException().mensajeLegible())
    }

    // ------------------- Integración con el resto de la app -------------------

    @Test
    fun `el repositorio encuentra al usuario con la extension`() {
        assertNotNull(UsuariosRepository.buscarPorCorreo(" JOSEFA@gmail.com "))
        assertNotNull(UsuariosRepository.validarAcceso("JOSEFA@gmail.com", "hola1234"))
        assertNull(UsuariosRepository.validarAcceso("josefa@gmail.com", "otra clave"))
    }

    @Test
    fun `el usuario registrado queda con el correo normalizado`() {
        val nuevo = Usuario(
            nombre = "Raúl Low",
            correo = "  raul@gmail.com  ",
            password = "hola1234",
            tipoUsuario = TipoUsuario.PROFESIONAL,
            medioPreferido = MedioComunicacion.TEXTO_ESCRITO
        )

        assertTrue(UsuariosRepository.agregarUsuario(nuevo))
        assertEquals("raul@gmail.com", UsuariosRepository.buscarPorCorreo("raul@gmail.com")?.correo)
    }

    @Test
    fun `comoFrase deja pareja la frase que se guarda`() {
        assertEquals("Necesito un intérprete", "  necesito   un   intérprete ".comoFrase())
        assertEquals("Necesito un intérprete", "necesito un intérprete".comoFrase())
    }
}
