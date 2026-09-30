package com.vozinha.app

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vozinha.app.data.servicios.Localizador
import com.vozinha.app.data.servicios.Resultado
import com.vozinha.app.ui.components.Vibrador
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Pruebas con Robolectric.
 *
 * Robolectric levanta el entorno de Android dentro de la JVM, de modo que se
 * puede probar el código que depende del sistema operativo sin conectar un
 * teléfono ni esperar a que arranque un emulador.
 *
 * Aquí se prueba lo que las pruebas anteriores no alcanzan: el manejo de los
 * permisos de ubicación y que el vibrador no rompa la aplicación cuando el
 * equipo no tiene el servicio.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RobolectricTest {

    private lateinit var contexto: Context
    private lateinit var localizador: Localizador

    @Before
    fun preparar() {
        contexto = ApplicationProvider.getApplicationContext()
        localizador = Localizador(contexto)
    }

    private fun aplicacion() = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `sin permiso concedido el localizador lo informa`() {
        shadowOf(aplicacion()).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        assertFalse(localizador.hayPermiso())
    }

    @Test
    fun `basta con el permiso aproximado para leer la ubicacion`() {
        shadowOf(aplicacion()).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(aplicacion()).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

        assertTrue(localizador.hayPermiso())
    }

    @Test
    fun `sin permiso la lectura devuelve un fallo explicado y no una excepcion`() = runTest {
        shadowOf(aplicacion()).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val resultado = localizador.ubicacionActual()

        assertTrue(resultado is Resultado.Fallo)
        assertEquals(
            "Necesitamos tu permiso para acceder a la ubicación",
            (resultado as Resultado.Fallo).mensaje
        )
    }

    @Test
    fun `el localizador declara los dos permisos que admite el sistema`() {
        assertEquals(2, Localizador.PERMISOS.size)
        assertTrue(Localizador.PERMISOS.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(Localizador.PERMISOS.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @Test
    fun `vibrar no lanza excepcion aunque el equipo no tenga vibrador`() {
        // El entorno de prueba no tiene motor de vibración: la llamada tiene
        // que resolverse en silencio gracias al try/catch de la función.
        Vibrador.confirmar(contexto)
        Vibrador.alertar(contexto)
    }

    @Test
    fun `la aplicacion declara los permisos que necesita`() {
        val permisos = contexto.packageManager
            .getPackageInfo(contexto.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
            .toList()

        assertTrue(permisos.contains(Manifest.permission.INTERNET))
        assertTrue(permisos.contains(Manifest.permission.VIBRATE))
        assertTrue(permisos.contains(Manifest.permission.ACCESS_FINE_LOCATION))
    }
}
