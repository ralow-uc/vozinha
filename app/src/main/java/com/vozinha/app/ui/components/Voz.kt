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

    private fun vibrar(context: Context, patron: LongArray) {
        val vibrador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrador == null || !vibrador.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrador.vibrate(VibrationEffect.createWaveform(patron, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrador.vibrate(patron, -1)
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

    private var motor: TextToSpeech? = null

    init {
        // La referencia se captura en una variable local porque la devolución
        // de llamada puede ejecutarse antes de que termine el constructor.
        var referencia: TextToSpeech? = null
        referencia = TextToSpeech(context.applicationContext) { estado ->
            val tts = referencia
            if (estado != TextToSpeech.SUCCESS || tts == null) {
                disponible = false
                return@TextToSpeech
            }
            val resultado = tts.setLanguage(Locale.forLanguageTag("es-CL"))
            idiomaDisponible = resultado != TextToSpeech.LANG_MISSING_DATA &&
                resultado != TextToSpeech.LANG_NOT_SUPPORTED
            if (!idiomaDisponible) {
                tts.language = Locale.getDefault()
            }
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    hablando = true
                }

                override fun onDone(utteranceId: String?) {
                    hablando = false
                }

                // La firma sin código de error está obsoleta, pero la clase base
                // la declara abstracta y obliga a implementarla.
                @Suppress("OVERRIDE_DEPRECATION")
                override fun onError(utteranceId: String?) {
                    hablando = false
                }
            })
            listo = true
        }
        motor = referencia
    }

    /** Reproduce el texto. Devuelve false si el motor no está listo. */
    fun hablar(texto: String): Boolean {
        val contenido = texto.trim()
        if (!listo || contenido.isEmpty()) return false
        val resultado = motor?.speak(contenido, TextToSpeech.QUEUE_FLUSH, null, ID_FRASE)
        return resultado == TextToSpeech.SUCCESS
    }

    fun detener() {
        motor?.stop()
        hablando = false
    }

    fun liberar() {
        motor?.stop()
        motor?.shutdown()
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
