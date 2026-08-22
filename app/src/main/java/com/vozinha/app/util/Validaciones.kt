package com.vozinha.app.util



/**
 * Reglas de validación de los formularios.
 *
 * Están separadas de la interfaz para poder probarlas con tests unitarios sin
 * levantar la UI.
 */
object Validaciones {

    const val LARGO_MINIMO_PASSWORD = 6

    fun esEmailValido(email: String): Boolean {
        val limpio = email.trim()
        if (limpio.isEmpty()) return false
        return Regex("^[\\w.+-]+@[\\w-]+\\.[A-Za-z]{2,}$").matches(limpio)
    }

    fun esPasswordValida(password: String): Boolean = password.length >= LARGO_MINIMO_PASSWORD

    fun esNombreValido(nombre: String): Boolean = nombre.trim().length >= 3

    fun coincidenPasswords(password: String, confirmacion: String): Boolean =
        password.isNotEmpty() && password == confirmacion

    fun errorEmail(email: String): String? = when {
        email.isBlank() -> "Escribe tu correo electrónico"
        !email.contains("@") -> "El correo debe incluir el símbolo @"
        !esEmailValido(email) -> "Revisa el correo, por ejemplo: nombre@gmail.com"
        else -> null
    }

    fun errorPassword(password: String): String? = when {
        password.isBlank() -> "Escribe tu contraseña"
        !esPasswordValida(password) ->
            "La contraseña necesita al menos $LARGO_MINIMO_PASSWORD caracteres"
        else -> null
    }

    fun errorNombre(nombre: String): String? = when {
        nombre.isBlank() -> "Escribe tu nombre"
        !esNombreValido(nombre) -> "El nombre necesita al menos 3 letras"
        else -> null
    }

    fun errorConfirmacion(password: String, confirmacion: String): String? = when {
        confirmacion.isBlank() -> "Repite tu contraseña"
        !coincidenPasswords(password, confirmacion) -> "Las dos contraseñas no son iguales"
        else -> null
    }
}
