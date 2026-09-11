package com.vozinha.app.data

import com.vozinha.app.util.conCorreo
import com.vozinha.app.util.normalizado


/**
 * Arreglo de usuarios de la aplicación.
 *
 * El requerimiento pide un array que almacene, desde la view de Registro,
 * cinco usuarios con sus respectivas contraseñas de acceso. El arreglo parte
 * con cinco usuarios y la view de Registro agrega los nuevos. Los datos viven
 * en memoria: al cerrar la aplicación vuelven a su estado inicial.
 */
object UsuariosRepository {

    const val USUARIOS_INICIALES = 5

    private val iniciales: Array<Usuario> = arrayOf(
        Usuario("Camila Rojas", "camila@gmail.com", "hola1234", TipoUsuario.PERSONA_SORDA, MedioComunicacion.LENGUA_DE_SENAS),
        Usuario("Diego Fuentes", "diego@gmail.com", "hola1234", TipoUsuario.PERSONA_HIPOACUSIA, MedioComunicacion.TEXTO_ESCRITO),
        Usuario("Valentina Soto", "valentina@gmail.com", "hola1234", TipoUsuario.INTERPRETE, MedioComunicacion.LENGUA_DE_SENAS),
        Usuario("Matías Herrera", "matias@gmail.com", "hola1234", TipoUsuario.FAMILIAR, MedioComunicacion.LECTURA_LABIAL),
        Usuario("Josefa Muñoz", "josefa@gmail.com", "hola1234", TipoUsuario.PROFESIONAL, MedioComunicacion.TEXTO_ESCRITO)
    )

    private val registrados = iniciales.toMutableList()

    fun obtenerUsuarios(): List<Usuario> = registrados.toList()

    /** Indica si el correo ya está tomado, sin distinguir mayúsculas. */
    fun existeCorreo(correo: String): Boolean = buscarPorCorreo(correo) != null

    /** Agrega un usuario. Devuelve false si el correo ya estaba registrado. */
    fun agregarUsuario(usuario: Usuario): Boolean {
        if (existeCorreo(usuario.correo)) return false
        registrados.add(usuario.copy(correo = usuario.correo.normalizado()))
        return true
    }

    /**
     * Valida las credenciales de acceso. Devuelve el usuario o null.
     *
     * takeIf deja la expresión en null cuando la contraseña no calza, de modo
     * que el correo inexistente y la clave equivocada se resuelvan igual.
     */
    fun validarAcceso(correo: String, password: String): Usuario? =
        buscarPorCorreo(correo)?.takeIf { it.password == password }

    /**
     * Busca un usuario por su correo.
     *
     * Las tres búsquedas del repositorio se apoyan en la misma función de
     * extensión, así la comparación de correos está escrita una sola vez.
     */
    fun buscarPorCorreo(correo: String): Usuario? = registrados.conCorreo(correo)

    /** Deja el arreglo con los cinco usuarios iniciales. */
    fun restaurar() {
        registrados.clear()
        registrados.addAll(iniciales)
    }
}
