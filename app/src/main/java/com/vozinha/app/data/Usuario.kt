package com.vozinha.app.data



/**
 * Tipo de usuario que se registra en la aplicación.
 *
 * Es un enum y no una cadena de texto para que las comparaciones no dependan
 * de cómo esté escrito el nombre y para trabajar con valores controlados.
 */
enum class TipoUsuario(val etiqueta: String) {
    PERSONA_SORDA("Persona sorda"),
    PERSONA_HIPOACUSIA("Persona con hipoacusia"),
    INTERPRETE("Intérprete de lengua de señas"),
    FAMILIAR("Familiar o acompañante"),
    PROFESIONAL("Docente o profesional")
}

/** Medio de comunicación que la persona prefiere usar. */
enum class MedioComunicacion(val etiqueta: String) {
    TEXTO_ESCRITO("Texto escrito"),
    LENGUA_DE_SENAS("Lengua de señas"),
    LECTURA_LABIAL("Lectura labial")
}

/**
 * Usuario registrado en la aplicación.
 *
 * La contraseña se guarda junto al usuario porque esta entrega no contempla
 * servidor ni cifrado: el objetivo es demostrar el manejo del arreglo de
 * usuarios que alimenta la view de Registro.
 */
data class Usuario(
    val nombre: String,
    val correo: String,
    val password: String,
    val tipoUsuario: TipoUsuario,
    val medioPreferido: MedioComunicacion
)
