package com.vozinha.app.data.servicios

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.vozinha.app.data.Ubicacion
import kotlinx.coroutines.tasks.await

/**
 * Obtiene la ubicación del teléfono para la view BuscarDispositivo.
 *
 * Usa el proveedor fusionado de Google Play Services, que combina GPS, redes y
 * sensores para entregar una posición con el menor gasto de batería posible.
 *
 * La llamada al proveedor devuelve una Task. La extensión KTX await() de
 * kotlinx-coroutines-play-services la convierte en una función de suspensión,
 * de modo que el código se lea de arriba abajo en lugar de encadenar
 * devoluciones de llamada.
 */
class Localizador(private val contexto: Context) {

    private val proveedor by lazy {
        LocationServices.getFusedLocationProviderClient(contexto.applicationContext)
    }

    /**
     * Indica si la persona ya concedió alguno de los dos permisos de ubicación.
     *
     * ContextCompat.checkSelfPermission viene de core-ktx y resuelve por sí
     * misma las diferencias entre versiones de Android.
     */
    fun hayPermiso(): Boolean = PERMISOS.any { permiso ->
        ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Pide una lectura nueva de la ubicación.
     *
     * Devuelve un fallo descriptivo en lugar de lanzar excepción, para que la
     * pantalla pueda escribir en palabras qué ocurrió: quien usa la aplicación
     * no puede oír un aviso.
     */
    suspend fun ubicacionActual(etiqueta: String = ""): Resultado<Ubicacion> {
        if (!hayPermiso()) {
            return Resultado.Fallo("Necesitamos tu permiso para acceder a la ubicación")
        }
        return try {
            val peticion = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setMaxUpdateAgeMillis(ANTIGUEDAD_MAXIMA)
                .build()

            @Suppress("MissingPermission")
            val posicion = proveedor.getCurrentLocation(peticion, null).await()
                ?: return Resultado.Fallo("No pudimos leer la ubicación. Revisa que el GPS esté encendido.")

            Resultado.Exito(
                Ubicacion(
                    latitud = posicion.latitude,
                    longitud = posicion.longitude,
                    precisionMetros = posicion.accuracy,
                    registradaEn = System.currentTimeMillis(),
                    etiqueta = etiqueta
                )
            )
        } catch (error: SecurityException) {
            Resultado.Fallo("El permiso de ubicación fue revocado")
        } catch (error: Exception) {
            Resultado.Fallo(error.message ?: "No pudimos leer la ubicación")
        }
    }

    companion object {
        /** Los dos permisos que el sistema admite para leer la ubicación. */
        val PERMISOS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        private const val ANTIGUEDAD_MAXIMA = 30_000L
    }
}
