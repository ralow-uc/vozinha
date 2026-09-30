package com.vozinha.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vozinha.app.data.Mensaje
import com.vozinha.app.data.Ubicacion
import com.vozinha.app.data.Usuario
import com.vozinha.app.data.UsuariosRepository
import com.vozinha.app.data.servicios.Backend
import com.vozinha.app.data.servicios.Resultado
import com.vozinha.app.data.servicios.backendActivo
import com.vozinha.app.data.servicios.error
import com.vozinha.app.data.servicios.oBien
import com.vozinha.app.util.comoFrase
import kotlinx.coroutines.launch

/** Resultado de intentar registrar un usuario nuevo. */
sealed interface ResultadoRegistro {
    data class Exitoso(val totalUsuarios: Int) : ResultadoRegistro
    data class Rechazado(val motivo: String) : ResultadoRegistro
}

/**
 * Estado de la aplicación: sesión, usuarios, mensajes y ubicaciones.
 *
 * Habla con el backend a través de las interfaces de servicio, nunca con
 * Firebase directamente, así la misma clase sirve con la base de datos remota
 * y con la implementación local que se usa sin conexión y en las pruebas.
 *
 * Cada operación se lanza en viewModelScope, el alcance de corrutinas que
 * aporta la extensión KTX lifecycle-viewmodel-ktx: el trabajo pendiente se
 * cancela solo cuando el ViewModel deja de existir.
 */
class AppViewModel(private val backend: Backend = backendActivo()) : ViewModel() {

    /** Nombre del backend en uso, que la aplicación muestra en pantalla. */
    val nombreBackend: String = backend.nombre

    var usuarios by mutableStateOf(UsuariosRepository.obtenerUsuarios())
        private set

    var sesionActiva by mutableStateOf<Usuario?>(null)
        private set

    var mensajes by mutableStateOf(emptyList<Mensaje>())
        private set

    var ubicaciones by mutableStateOf(emptyList<Ubicacion>())
        private set

    /** Hay una operación contra el backend en curso. */
    var ocupado by mutableStateOf(false)
        private set

    val totalUsuarios: Int
        get() = usuarios.size

    private val correoSesion: String
        get() = sesionActiva?.correo.orEmpty()

    // ----------------------------------------------------------------- acceso

    /**
     * Autentica y, si el acceso es correcto, carga los datos de la persona.
     *
     * [onListo] recibe null cuando todo salió bien, o el mensaje de error.
     */
    fun iniciarSesion(correo: String, password: String, onListo: (String?) -> Unit) {
        lanzar(onListo) {
            val acceso = backend.autenticacion.iniciarSesion(correo, password)
            if (acceso is Resultado.Fallo) return@lanzar acceso.mensaje

            val ficha = backend.usuarios.obtener(correo).oBien(null)
                ?: UsuariosRepository.buscarPorCorreo(correo)
                ?: return@lanzar "No encontramos la ficha de esa cuenta"

            sesionActiva = ficha
            recargarDatosDeLaSesion()
            null
        }
    }

    /** Crea la cuenta en el servicio de autenticación y su ficha en la base. */
    fun registrar(usuario: Usuario, onListo: (ResultadoRegistro) -> Unit) {
        viewModelScope.launch {
            ocupado = true
            try {
                val alta = backend.autenticacion.registrar(usuario.correo, usuario.password)
                if (alta is Resultado.Fallo) {
                    onListo(ResultadoRegistro.Rechazado(alta.mensaje))
                    return@launch
                }
                val ficha = backend.usuarios.crear(usuario)
                if (ficha is Resultado.Fallo) {
                    onListo(ResultadoRegistro.Rechazado(ficha.mensaje))
                    return@launch
                }
                usuarios = backend.usuarios.listar().oBien(UsuariosRepository.obtenerUsuarios())
                onListo(ResultadoRegistro.Exitoso(usuarios.size))
            } finally {
                ocupado = false
            }
        }
    }

    /** Pide al backend que envíe el correo con los pasos de recuperación. */
    fun recuperarPassword(correo: String, onListo: (String?) -> Unit) {
        lanzar(onListo) { backend.autenticacion.enviarCorreoDeRecuperacion(correo).error }
    }

    fun correoRegistrado(correo: String): Boolean =
        UsuariosRepository.buscarPorCorreo(correo) != null

    fun cerrarSesion() {
        backend.autenticacion.cerrarSesion()
        sesionActiva = null
        mensajes = emptyList()
        ubicaciones = emptyList()
    }

    // --------------------------------------------------- mensajes (Escribir)

    /** Guarda un mensaje nuevo. Completa la letra C del CRUD. */
    fun guardarMensaje(texto: String, onListo: (String?) -> Unit) {
        lanzar(onListo) {
            val guardado = backend.mensajes.guardar(correoSesion, texto.comoFrase())
            if (guardado is Resultado.Fallo) return@lanzar guardado.mensaje
            recargarMensajes()
            null
        }
    }

    /** Suma una reproducción al mensaje. Completa la letra U del CRUD. */
    fun registrarReproduccion(id: String) {
        lanzar({}) {
            backend.mensajes.registrarReproduccion(correoSesion, id)
            recargarMensajes()
            null
        }
    }

    /** Marca o desmarca el mensaje como favorito. También es una actualización. */
    fun cambiarFavorito(id: String, onListo: (String?) -> Unit = {}) {
        lanzar(onListo) {
            val cambio = backend.mensajes.cambiarFavorito(correoSesion, id)
            if (cambio is Resultado.Fallo) return@lanzar cambio.mensaje
            recargarMensajes()
            null
        }
    }

    /** Borra el mensaje. Completa la letra D del CRUD. */
    fun eliminarMensaje(id: String, onListo: (String?) -> Unit = {}) {
        lanzar(onListo) {
            val borrado = backend.mensajes.eliminar(correoSesion, id)
            if (borrado is Resultado.Fallo) return@lanzar borrado.mensaje
            recargarMensajes()
            null
        }
    }

    // --------------------------------------- ubicaciones (BuscarDispositivo)

    /** Guarda una lectura de ubicación en la base de datos. */
    fun guardarUbicacion(ubicacion: Ubicacion, onListo: (String?) -> Unit) {
        lanzar(onListo) {
            val guardada = backend.ubicaciones.guardar(correoSesion, ubicacion)
            if (guardada is Resultado.Fallo) return@lanzar guardada.mensaje
            recargarUbicaciones()
            null
        }
    }

    fun borrarHistorialDeUbicaciones(onListo: (String?) -> Unit = {}) {
        lanzar(onListo) {
            backend.ubicaciones.borrarHistorial(correoSesion)
            recargarUbicaciones()
            null
        }
    }

    // ------------------------------------------------------------- auxiliares

    private suspend fun recargarDatosDeLaSesion() {
        recargarMensajes()
        recargarUbicaciones()
    }

    private suspend fun recargarMensajes() {
        mensajes = backend.mensajes.listar(correoSesion).oBien(emptyList())
    }

    private suspend fun recargarUbicaciones() {
        ubicaciones = backend.ubicaciones.listar(correoSesion).oBien(emptyList())
    }

    /**
     * Ejecuta la operación en el alcance del ViewModel y devuelve el error al
     * llamador, dejando marcado el estado de ocupado mientras dura.
     */
    private fun lanzar(onListo: (String?) -> Unit, bloque: suspend () -> String?) {
        viewModelScope.launch {
            ocupado = true
            try {
                onListo(bloque())
            } finally {
                ocupado = false
            }
        }
    }
}
