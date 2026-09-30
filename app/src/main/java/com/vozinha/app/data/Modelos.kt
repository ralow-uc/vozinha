package com.vozinha.app.data

import java.util.Locale

/**
 * Mensaje que la persona escribió en la view Escribir para decirlo en voz alta.
 *
 * Se guarda en la base de datos para que la frase siga disponible al cambiar de
 * teléfono y para que la view Hablar pueda reproducirla sin volver a escribirla.
 */
data class Mensaje(
    val id: String = "",
    val texto: String = "",
    val creadoEn: Long = 0L,
    val vecesDicho: Int = 0,
    val favorito: Boolean = false
) {
    /** Constructor vacío que Firestore necesita para reconstruir el objeto. */
    constructor() : this("", "", 0L, 0, false)
}

/**
 * Ubicación registrada desde la view BuscarDispositivo.
 *
 * Para una persona sorda, poder mostrar dónde está en un mapa reemplaza a la
 * explicación hablada cuando necesita que alguien la vaya a buscar.
 */
data class Ubicacion(
    val id: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val precisionMetros: Float = 0f,
    val registradaEn: Long = 0L,
    val etiqueta: String = ""
) {
    constructor() : this("", 0.0, 0.0, 0f, 0L, "")

    /**
     * Coordenadas en el formato que entienden las aplicaciones de mapas.
     *
     * Se fuerza la configuración regional de Estados Unidos porque las
     * coordenadas se escriben siempre con punto decimal. Con la configuración
     * del teléfono en español saldrían con coma y el mapa no las reconocería.
     */
    val coordenadas: String
        get() = String.format(Locale.US, "%.5f, %.5f", latitud, longitud)

    /** Enlace que abre la ubicación en una aplicación de mapas. */
    val enlaceMapa: String
        get() = "https://maps.google.com/?q=$latitud,$longitud"
}
