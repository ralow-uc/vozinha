package com.vozinha.app.util

import com.vozinha.app.data.Usuario

/*
 * Utilidades de Kotlin que usan las views: funciones y propiedades de
 * extensión, funciones de orden superior y el manejo de errores.
 */

// ---------------------------------------------------------------------------
// Funciones y propiedades de extensión
// ---------------------------------------------------------------------------

/**
 * Deja el texto sin espacios al inicio ni al final y sin espacios repetidos en
 * el medio.
 *
 * Es una función de extensión sobre String: no modifica la clase original,
 * solo permite llamarla con notación de punto sobre cualquier cadena.
 */
fun String.normalizado(): String = trim().replace(Regex("\\s+"), " ")

/**
 * Propiedad de extensión que indica si el texto tiene forma de correo.
 *
 * Se declara con get() porque las propiedades de extensión no guardan valor:
 * lo calculan cada vez que se consultan.
 */
val String.esCorreo: Boolean
    get() = Validaciones.esEmailValido(this)

/**
 * Deja la frase lista para guardarla: sin espacios sobrantes y con la primera
 * letra en mayúscula, para que la lista se vea pareja.
 */
fun String.comoFrase(): String =
    normalizado().replaceFirstChar { primera -> primera.uppercase() }

/**
 * Busca en la lista el usuario que tiene ese correo, sin distinguir mayúsculas.
 *
 * Al ser una extensión sobre List<Usuario>, la misma búsqueda sirve para el
 * inicio de sesión, el registro y la recuperación de contraseña.
 */
fun List<Usuario>.conCorreo(correo: String): Usuario? {
    val buscado = correo.normalizado()
    return firstOrNull { it.correo.equals(buscado, ignoreCase = true) }
}

/** Propiedad de extensión con la descripción corta del usuario. */
val Usuario.resumen: String
    get() = "$nombre, ${tipoUsuario.etiqueta.lowercase()}"

// ---------------------------------------------------------------------------
// Funciones de orden superior
// ---------------------------------------------------------------------------

/**
 * Tipo de función que revisa un campo y devuelve el mensaje de error, o null
 * cuando el dato está correcto.
 */
typealias ReglaDeCampo = (String) -> String?

/** Lo que cada view le exige al correo que la persona escribe. */
enum class ExigenciaCorreo {
    /** Basta con que tenga formato de correo. Lo usa el inicio de sesión. */
    SOLO_FORMATO,

    /** Además, nadie más puede tenerlo. Lo usa el registro. */
    DEBE_SER_NUEVO,

    /** Además, tiene que existir. Lo usa la recuperación de contraseña. */
    DEBE_EXISTIR
}

/**
 * Construye la regla que valida el correo según lo que la view necesite.
 *
 * Es una función de orden superior por partida doble: recibe una función que
 * consulta si el correo ya está registrado y devuelve otra función, la regla
 * misma. Así las tres views comparten la validación de formato y cada una
 * agrega solo su condición propia.
 */
fun reglaDeCorreo(
    exigencia: ExigenciaCorreo,
    estaRegistrado: (String) -> Boolean = { false }
): ReglaDeCampo = { correo ->
    Validaciones.errorEmail(correo) ?: when (exigencia) {
        ExigenciaCorreo.SOLO_FORMATO -> null
        ExigenciaCorreo.DEBE_SER_NUEVO ->
            if (estaRegistrado(correo)) "Ese correo ya tiene una cuenta" else null

        ExigenciaCorreo.DEBE_EXISTIR ->
            if (estaRegistrado(correo)) null else "No encontramos una cuenta con ese correo"
    }
}

/**
 * Aplica las reglas en el orden en que vienen y devuelve el primer error.
 *
 * Recibe funciones como argumento, de modo que cada regla se evalúa solo si
 * las anteriores pasaron. Devuelve null cuando el formulario está completo.
 */
fun primerError(vararg reglas: () -> String?): String? =
    reglas.firstNotNullOfOrNull { regla -> regla() }

// ---------------------------------------------------------------------------
// Gestión de errores
// ---------------------------------------------------------------------------

/**
 * Ejecuta el bloque y, si el dispositivo lanza una excepción, la entrega a
 * [alFallar] en lugar de dejar caer la aplicación.
 *
 * Se declara inline para que el compilador copie el cuerpo en cada llamada y
 * no cree un objeto por cada lambda. La usan el motor de voz y el vibrador,
 * que dependen de servicios del teléfono que pueden no estar disponibles.
 */
inline fun <T> intentar(alFallar: (Exception) -> Unit = {}, bloque: () -> T): T? =
    try {
        bloque()
    } catch (error: Exception) {
        alFallar(error)
        null
    }

/** Mensaje corto de la excepción, para mostrarlo en pantalla. */
fun Exception.mensajeLegible(): String =
    message?.normalizado()?.takeIf { it.isNotEmpty() } ?: this::class.simpleName.orEmpty()
