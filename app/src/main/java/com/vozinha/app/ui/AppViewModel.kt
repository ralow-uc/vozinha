package com.vozinha.app.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.vozinha.app.data.FRASES_INICIALES
import com.vozinha.app.data.Usuario
import com.vozinha.app.data.UsuariosRepository
import com.vozinha.app.util.comoFrase

/** Resultado de intentar registrar un usuario nuevo. */
sealed interface ResultadoRegistro {
    data class Exitoso(val totalUsuarios: Int) : ResultadoRegistro
    data object CorreoRepetido : ResultadoRegistro
}

/**
 * Estado de la aplicación: usuarios, sesión activa y frases rápidas.
 *
 * Vive fuera de las pantallas para que el arreglo que consulta el login sea el
 * mismo que alimenta el registro, y para que la sesión y las frases sobrevivan
 * a los giros de pantalla y a la navegación entre views.
 */
class AppViewModel : ViewModel() {

    var usuarios by mutableStateOf(UsuariosRepository.obtenerUsuarios())
        private set

    var sesionActiva by mutableStateOf<Usuario?>(null)
        private set

    val totalUsuarios: Int
        get() = usuarios.size

    fun iniciarSesion(correo: String, password: String): Boolean {
        val usuario = UsuariosRepository.validarAcceso(correo, password) ?: return false
        sesionActiva = usuario
        return true
    }

    fun registrar(usuario: Usuario): ResultadoRegistro {
        if (!UsuariosRepository.agregarUsuario(usuario)) return ResultadoRegistro.CorreoRepetido
        usuarios = UsuariosRepository.obtenerUsuarios()
        return ResultadoRegistro.Exitoso(usuarios.size)
    }

    fun correoRegistrado(correo: String): Boolean =
        UsuariosRepository.buscarPorCorreo(correo) != null

    fun cerrarSesion() {
        sesionActiva = null
    }

    /** Frases rápidas disponibles en la view de comunicación. */
    var frases by mutableStateOf(FRASES_INICIALES)
        private set

    /**
     * Agrega una frase a la lista.
     *
     * Devuelve false si la frase está vacía o si ya existe, para que la
     * pantalla avise en lugar de duplicarla en silencio.
     */
    fun agregarFrase(texto: String): Boolean {
        val limpia = texto.comoFrase()
        if (limpia.isEmpty()) return false
        if (frases.any { it.equals(limpia, ignoreCase = true) }) return false
        frases = frases + limpia
        return true
    }

    /** Quita una frase de la lista. */
    fun eliminarFrase(frase: String) {
        frases = frases.filterNot { it == frase }
    }
}
