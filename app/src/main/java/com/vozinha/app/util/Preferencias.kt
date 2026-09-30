package com.vozinha.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Preferencias que se guardan en el propio teléfono.
 *
 * preferencesDataStore es una propiedad de extensión sobre Context que aporta
 * la extensión KTX de DataStore: crea el almacén una sola vez por proceso y lo
 * deja disponible con notación de punto, sin que la aplicación tenga que
 * administrar su ciclo de vida.
 */
private val Context.almacen: DataStore<Preferences> by preferencesDataStore(name = "vozinha")

/**
 * Recuerda el correo de la última persona que ingresó.
 *
 * DataStore entrega los datos como un Flow, de modo que la pantalla se entera
 * del cambio sin volver a consultar el disco, y escribe con funciones de
 * suspensión, así la lectura nunca bloquea el hilo de la interfaz.
 */
class PreferenciasUsuario(private val contexto: Context) {

    /** Correo guardado, o cadena vacía si todavía no hay ninguno. */
    val correoRecordado: Flow<String> = contexto.almacen.data
        .catch { error ->
            // Si el archivo se corrompe, se parte de cero en lugar de caerse.
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferencias -> preferencias[CLAVE_CORREO].orEmpty() }

    suspend fun recordarCorreo(correo: String) {
        contexto.almacen.edit { preferencias ->
            preferencias[CLAVE_CORREO] = correo.normalizado()
        }
    }

    suspend fun olvidarCorreo() {
        contexto.almacen.edit { preferencias -> preferencias.remove(CLAVE_CORREO) }
    }

    private companion object {
        val CLAVE_CORREO = stringPreferencesKey("correo_recordado")
    }
}
