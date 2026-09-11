package com.vozinha.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.vozinha.app.util.intentar
import com.vozinha.app.util.mensajeLegible
import com.vozinha.app.util.normalizado
import java.util.Locale

/**
 * Vibración del teléfono.
 *
 * Es la prestación del dispositivo más importante de esta aplicación: para una
 * persona sorda, la vibración reemplaza al aviso sonoro que confirma que una
 * acción se completó.
 */
object Vibrador {

    fun confirmar(context: Context) = vibrar(context, longArrayOf(0, 120))

    fun alertar(context: Context) = vibrar(context, longArrayOf(0, 200, 120, 200))

    /**
     * Hace vibrar el teléfono con el patrón indicado.
     *
     * Toda la llamada va dentro de intentar porque el servicio de vibración
     * puede no existir en el equipo o negar el permiso: si eso ocurre la
     * aplicación sigue funcionando sin vibración, en vez de caerse.
     */
    private fun vibrar(context: Context, patron: LongArray) {
        intentar {
            val vibrador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                    ?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrador == null || !vibrador.hasVibrator()) return@intentar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrador.vibrate(VibrationEffect.createWaveform(patron, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrador.vibrate(patron, -1)
            }
        }
    }
}

/**
 * Motor de texto a voz del sistema.
 *
 * Expone el estado de inicialización como estado de Compose porque la persona
 * que usa la aplicación no puede oír si el motor funcionó: la pantalla tiene
 * que decírselo.
 */
class SintetizadorVoz(context: Context) {

    /** El motor terminó de inicializarse. */
    var listo by mutableStateOf(false)
        private set

    /** El teléfono tiene un motor de texto a voz instalado. */
    var disponible by mutableStateOf(true)
        private set

    /** El motor tiene instalada la voz en español. */
    var idiomaDisponible by mutableStateOf(true)
        private set

    /** Hay una frase reproduciéndose en este momento. */
    var hablando by mutableStateOf(false)
        private set

    /**
     * Descripción del último fallo del motor, o null si no ha habido ninguno.
     *
     * La persona que usa la aplicación no puede oír que la reproducción falló,
     * así que el error tiene que quedar escrito en la pantalla.
     */
    var ultimoError by mutableStateOf<String?>(null)
        private set

    private var motor: TextToSpeech? = null

    init {
        // La referencia se captura en una variable local porque la devolución
        // de llamada puede ejecutarse antes de que termine el constructor.
        var referencia: TextToSpeech? = null
        // Construir el motor toca un servicio del sistema: si el teléfono no
        // trae ninguno instalado, la llamada lanza excepción y la aplicación
        // se limita a marcar el motor como no disponible.
        referencia = intentar(alFallar = { error -> registrarFallo(error) }) {
            TextToSpeech(context.applicationContext) { estado ->
                val tts = referencia
                if (estado != TextToSpeech.SUCCESS || tts == null) {
                    disponible = false
                    return@TextToSpeech
                }
                configurarIdioma(tts)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        hablando = true
                    }

                    override fun onDone(utteranceId: String?) {
                        hablando = false
                    }

                    // La firma sin código de error está obsoleta, pero la clase
                    // base la declara abstracta y obliga a implementarla.
                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun onError(utteranceId: String?) {
                        hablando = false
                        ultimoError = "El motor no pudo reproducir la frase"
                    }
                })
                listo = true
            }
        }
        if (referencia == null) disponible = false
        motor = referencia
    }

    /**
     * Deja el motor en español de Chile, o en el idioma del sistema si esa voz
     * no está instalada. La consulta del idioma también puede lanzar excepción.
     */
    private fun configurarIdioma(tts: TextToSpeech) {
        val resultado = intentar(alFallar = { error -> registrarFallo(error) }) {
            tts.setLanguage(Locale.forLanguageTag("es-CL"))
        }
        idiomaDisponible = resultado != null &&
            resultado != TextToSpeech.LANG_MISSING_DATA &&
            resultado != TextToSpeech.LANG_NOT_SUPPORTED
        if (!idiomaDisponible) {
            intentar { tts.language = Locale.getDefault() }
        }
    }

    /** Guarda el fallo para que la pantalla pueda mostrarlo. */
    private fun registrarFallo(error: Exception) {
        ultimoError = error.mensajeLegible()
    }

    /** Reproduce el texto. Devuelve false si el motor no está listo o falló. */
    fun hablar(texto: String): Boolean {
        val contenido = texto.normalizado()
        if (!listo || contenido.isEmpty()) return false
        ultimoError = null
        val resultado = intentar(alFallar = { error -> registrarFallo(error) }) {
            motor?.speak(contenido, TextToSpeech.QUEUE_FLUSH, null, ID_FRASE)
        }
        return resultado == TextToSpeech.SUCCESS
    }

    fun detener() {
        intentar { motor?.stop() }
        hablando = false
    }

    fun liberar() {
        intentar {
            motor?.stop()
            motor?.shutdown()
        }
        motor = null
        listo = false
        hablando = false
    }

    private companion object {
        const val ID_FRASE = "vozinha"
    }
}

/** Crea el sintetizador y lo libera al salir de la pantalla. */
@Composable
fun recordarSintetizadorVoz(): SintetizadorVoz {
    val context = LocalContext.current
    val sintetizador = remember { SintetizadorVoz(context) }
    DisposableEffect(Unit) { onDispose { sintetizador.liberar() } }
    return sintetizador
}
