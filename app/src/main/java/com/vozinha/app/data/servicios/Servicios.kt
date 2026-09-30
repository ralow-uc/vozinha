package com.vozinha.app.data.servicios

import com.vozinha.app.data.Mensaje
import com.vozinha.app.data.Ubicacion
import com.vozinha.app.data.Usuario
import com.vozinha.app.data.UsuariosRepository
import com.vozinha.app.util.conCorreo
import com.vozinha.app.util.normalizado

/**
 * Resultado de una operación contra el backend.
 *
 * Obliga a quien llama a considerar el fallo: no hay forma de leer el dato sin
 * pasar por el when que distingue los dos casos.
 */
sealed interface Resultado<out T> {
    data class Exito<T>(val dato: T) : Resultado<T>
    data class Fallo(val mensaje: String) : Resultado<Nothing>
}

/** Devuelve el dato si la operación salió bien, o el valor de reemplazo. */
fun <T> Resultado<T>.oBien(reemplazo: T): T = when (this) {
    is Resultado.Exito -> dato
    is Resultado.Fallo -> reemplazo
}

/** Mensaje de error, o null si la operación salió bien. */
val Resultado<*>.error: String?
    get() = (this as? Resultado.Fallo)?.mensaje

// ---------------------------------------------------------------------------
// Contratos
// ---------------------------------------------------------------------------

/**
 * Autorización y autenticación de las personas registradas.
 *
 * Las views de acceso dependen de esta interfaz y no de Firebase, de modo que
 * la aplicación siga funcionando cuando el backend no está configurado y que
 * las pruebas puedan reemplazarla por una implementación falsa.
 */
interface ServicioAutenticacion {

    /** Correo de la sesión abierta, o null si nadie ingresó. */
    val correoActual: String?

    suspend fun registrar(correo: String, password: String): Resultado<String>

    suspend fun iniciarSesion(correo: String, password: String): Resultado<String>

    suspend fun enviarCorreoDeRecuperacion(correo: String): Resultado<Unit>

    fun cerrarSesion()
}

/** CRUD de la ficha de cada persona usuaria. */
interface ServicioUsuarios {
    suspend fun crear(usuario: Usuario): Resultado<Unit>
    suspend fun obtener(correo: String): Resultado<Usuario?>
    suspend fun listar(): Resultado<List<Usuario>>
    suspend fun actualizar(usuario: Usuario): Resultado<Unit>
    suspend fun eliminar(correo: String): Resultado<Unit>
}

/** CRUD de los mensajes de las views Escribir y Hablar. */
interface ServicioMensajes {
    suspend fun guardar(correo: String, texto: String): Resultado<Mensaje>
    suspend fun listar(correo: String): Resultado<List<Mensaje>>
    suspend fun registrarReproduccion(correo: String, id: String): Resultado<Unit>
    suspend fun cambiarFavorito(correo: String, id: String): Resultado<Unit>
    suspend fun eliminar(correo: String, id: String): Resultado<Unit>
}

/** CRUD de las ubicaciones de la view BuscarDispositivo. */
interface ServicioUbicaciones {
    suspend fun guardar(correo: String, ubicacion: Ubicacion): Resultado<Ubicacion>
    suspend fun listar(correo: String): Resultado<List<Ubicacion>>
    suspend fun ultima(correo: String): Resultado<Ubicacion?>
    suspend fun borrarHistorial(correo: String): Resultado<Unit>
}

/** Los cuatro servicios que necesita la aplicación. */
data class Backend(
    val autenticacion: ServicioAutenticacion,
    val usuarios: ServicioUsuarios,
    val mensajes: ServicioMensajes,
    val ubicaciones: ServicioUbicaciones,
    val nombre: String
)

// ---------------------------------------------------------------------------
// Implementación local, que trabaja en memoria
// ---------------------------------------------------------------------------

/**
 * Autenticación contra el arreglo de usuarios que vive en el proyecto.
 *
 * Es la implementación de respaldo: se usa cuando el teléfono no tiene
 * conexión o cuando Firebase todavía no está configurado, y es también la que
 * emplean las pruebas unitarias.
 */
class AutenticacionLocal : ServicioAutenticacion {

    override var correoActual: String? = null
        private set

    override suspend fun registrar(correo: String, password: String): Resultado<String> {
        val limpio = correo.normalizado()
        if (UsuariosRepository.existeCorreo(limpio)) {
            return Resultado.Fallo("Ese correo ya tiene una cuenta")
        }
        correoActual = limpio
        return Resultado.Exito(limpio)
    }

    override suspend fun iniciarSesion(correo: String, password: String): Resultado<String> {
        val usuario = UsuariosRepository.validarAcceso(correo, password)
            ?: return Resultado.Fallo("El correo o la contraseña no coinciden con ninguna cuenta")
        correoActual = usuario.correo
        return Resultado.Exito(usuario.correo)
    }

    override suspend fun enviarCorreoDeRecuperacion(correo: String): Resultado<Unit> =
        if (UsuariosRepository.existeCorreo(correo)) Resultado.Exito(Unit)
        else Resultado.Fallo("No encontramos una cuenta con ese correo")

    override fun cerrarSesion() {
        correoActual = null
    }
}

/** Ficha de usuario guardada en el arreglo del proyecto. */
class UsuariosLocal : ServicioUsuarios {

    override suspend fun crear(usuario: Usuario): Resultado<Unit> =
        if (UsuariosRepository.agregarUsuario(usuario)) Resultado.Exito(Unit)
        else Resultado.Fallo("Ese correo ya tiene una cuenta")

    override suspend fun obtener(correo: String): Resultado<Usuario?> =
        Resultado.Exito(UsuariosRepository.buscarPorCorreo(correo))

    override suspend fun listar(): Resultado<List<Usuario>> =
        Resultado.Exito(UsuariosRepository.obtenerUsuarios())

    override suspend fun actualizar(usuario: Usuario): Resultado<Unit> {
        val existente = UsuariosRepository.obtenerUsuarios().conCorreo(usuario.correo)
            ?: return Resultado.Fallo("No encontramos una cuenta con ese correo")
        UsuariosRepository.reemplazar(existente.correo, usuario)
        return Resultado.Exito(Unit)
    }

    override suspend fun eliminar(correo: String): Resultado<Unit> =
        if (UsuariosRepository.eliminarUsuario(correo)) Resultado.Exito(Unit)
        else Resultado.Fallo("No encontramos una cuenta con ese correo")
}

/** Mensajes guardados en memoria, agrupados por el correo de cada persona. */
class MensajesLocal : ServicioMensajes {

    private val porUsuario = mutableMapOf<String, MutableList<Mensaje>>()

    private fun bandeja(correo: String) =
        porUsuario.getOrPut(correo.normalizado().lowercase()) { mutableListOf() }

    override suspend fun guardar(correo: String, texto: String): Resultado<Mensaje> {
        val limpio = texto.normalizado()
        if (limpio.isEmpty()) return Resultado.Fallo("Escribe un mensaje antes de guardarlo")
        val bandeja = bandeja(correo)
        if (bandeja.any { it.texto.equals(limpio, ignoreCase = true) }) {
            return Resultado.Fallo("Ese mensaje ya está guardado")
        }
        val mensaje = Mensaje(
            id = "local-${bandeja.size + 1}-${System.currentTimeMillis()}",
            texto = limpio,
            creadoEn = System.currentTimeMillis()
        )
        bandeja.add(mensaje)
        return Resultado.Exito(mensaje)
    }

    override suspend fun listar(correo: String): Resultado<List<Mensaje>> =
        Resultado.Exito(bandeja(correo).sortedByDescending { it.creadoEn })

    override suspend fun registrarReproduccion(correo: String, id: String): Resultado<Unit> =
        cambiar(correo, id) { it.copy(vecesDicho = it.vecesDicho + 1) }

    override suspend fun cambiarFavorito(correo: String, id: String): Resultado<Unit> =
        cambiar(correo, id) { it.copy(favorito = !it.favorito) }

    private fun cambiar(correo: String, id: String, transformar: (Mensaje) -> Mensaje): Resultado<Unit> {
        val bandeja = bandeja(correo)
        val indice = bandeja.indexOfFirst { it.id == id }
        if (indice < 0) return Resultado.Fallo("No encontramos ese mensaje")
        bandeja[indice] = transformar(bandeja[indice])
        return Resultado.Exito(Unit)
    }

    override suspend fun eliminar(correo: String, id: String): Resultado<Unit> =
        if (bandeja(correo).removeAll { it.id == id }) Resultado.Exito(Unit)
        else Resultado.Fallo("No encontramos ese mensaje")
}

/** Ubicaciones guardadas en memoria. */
class UbicacionesLocal : ServicioUbicaciones {

    private val porUsuario = mutableMapOf<String, MutableList<Ubicacion>>()

    private fun historial(correo: String) =
        porUsuario.getOrPut(correo.normalizado().lowercase()) { mutableListOf() }

    override suspend fun guardar(correo: String, ubicacion: Ubicacion): Resultado<Ubicacion> {
        val historial = historial(correo)
        val guardada = ubicacion.copy(id = "local-${historial.size + 1}-${ubicacion.registradaEn}")
        historial.add(guardada)
        return Resultado.Exito(guardada)
    }

    override suspend fun listar(correo: String): Resultado<List<Ubicacion>> =
        Resultado.Exito(historial(correo).sortedByDescending { it.registradaEn })

    override suspend fun ultima(correo: String): Resultado<Ubicacion?> =
        Resultado.Exito(historial(correo).maxByOrNull { it.registradaEn })

    override suspend fun borrarHistorial(correo: String): Resultado<Unit> {
        historial(correo).clear()
        return Resultado.Exito(Unit)
    }
}

/**
 * Elige con qué backend trabaja la aplicación.
 *
 * Prefiere Firebase y cae al backend local cuando el proyecto todavía no tiene
 * el archivo google-services.json o cuando el servicio no responde, de modo
 * que la aplicación nunca quede inutilizable por una falla de configuración.
 */
fun backendActivo(): Backend = backendFirebase() ?: backendLocal()

/** Backend completo que funciona sin conexión ni configuración externa. */
fun backendLocal(): Backend = Backend(
    autenticacion = AutenticacionLocal(),
    usuarios = UsuariosLocal(),
    mensajes = MensajesLocal(),
    ubicaciones = UbicacionesLocal(),
    nombre = "Local (sin conexión)"
)
