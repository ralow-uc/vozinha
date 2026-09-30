package com.vozinha.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.vozinha.app.data.Ubicacion
import com.vozinha.app.data.servicios.Localizador
import com.vozinha.app.data.servicios.Resultado
import com.vozinha.app.ui.components.BarraSuperior
import com.vozinha.app.ui.components.BotonPrimario
import com.vozinha.app.ui.components.BotonSecundario
import com.vozinha.app.ui.components.ColumnaFormulario
import com.vozinha.app.ui.components.TablaDatos
import com.vozinha.app.ui.components.TituloSeccion
import com.vozinha.app.ui.components.Vibrador
import com.vozinha.app.ui.theme.VozinhaTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Fecha y hora en formato corto para el historial. */
private val FORMATO_FECHA = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CL"))

/**
 * View BuscarDispositivo: muestra dónde está el teléfono y lo guarda.
 *
 * Para una persona sorda, poder enseñar la ubicación en pantalla o enviarla
 * por un enlace reemplaza a la explicación hablada cuando necesita que alguien
 * la vaya a buscar o cuando extravía el equipo.
 *
 * El permiso se pide con rememberLauncherForActivityResult, la API de
 * activity-compose que envuelve el contrato del sistema en una lambda.
 */
@Composable
fun BuscarDispositivoScreen(
    ubicaciones: List<Ubicacion>,
    ocupado: Boolean,
    onGuardarUbicacion: (Ubicacion, (String?) -> Unit) -> Unit,
    onBorrarHistorial: () -> Unit,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val localizador = remember(context) { Localizador(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var hayPermiso by remember { mutableStateOf(localizador.hayPermiso()) }
    var buscando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun buscarYGuardar() {
        scope.launch {
            buscando = true
            error = null
            when (val lectura = localizador.ubicacionActual()) {
                is Resultado.Fallo -> {
                    error = lectura.mensaje
                    Vibrador.alertar(context)
                    buscando = false
                }

                is Resultado.Exito -> {
                    onGuardarUbicacion(lectura.dato) { fallo ->
                        buscando = false
                        if (fallo == null) {
                            Vibrador.confirmar(context)
                            scope.launch {
                                snackbarHostState.showSnackbar("Ubicación guardada")
                            }
                        } else {
                            error = fallo
                            Vibrador.alertar(context)
                        }
                    }
                }
            }
        }
    }

    val pedirPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { concedidos ->
        hayPermiso = concedidos.values.any { it }
        if (hayPermiso) {
            buscarYGuardar()
        } else {
            error = "Sin el permiso de ubicación no podemos mostrar dónde estás"
            Vibrador.alertar(context)
        }
    }

    val ultima = ubicaciones.maxByOrNull { it.registradaEn }

    Scaffold(
        topBar = { BarraSuperior("Buscar dispositivo", onVolver) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ColumnaFormulario(padding) {
            Text(
                text = "Guarda dónde estás para poder mostrárselo a alguien, o para " +
                    "encontrar el teléfono si lo dejaste en otro lugar.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = error.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            BotonPrimario(
                texto = if (buscando || ocupado) "Buscando..." else "Buscar dónde estoy",
                icono = Icons.Filled.MyLocation,
                habilitado = !buscando && !ocupado,
                onClick = {
                    if (hayPermiso) buscarYGuardar() else pedirPermiso.launch(Localizador.PERMISOS)
                }
            )

            if (ultima != null) {
                TituloSeccion("Tu última ubicación")

                TablaDatos(
                    encabezadoIzquierdo = "Dato",
                    encabezadoDerecho = "Valor",
                    filas = listOf(
                        "Coordenadas" to ultima.coordenadas,
                        "Precisión" to "${ultima.precisionMetros.toInt()} metros",
                        "Registrada" to FORMATO_FECHA.format(Date(ultima.registradaEn))
                    )
                )

                BotonSecundario(
                    texto = "Abrir en el mapa",
                    icono = Icons.Filled.Map,
                    onClick = {
                        val intento = Intent(Intent.ACTION_VIEW, ultima.enlaceMapa.toUri())
                        context.startActivity(Intent.createChooser(intento, "Ver la ubicación"))
                    }
                )
            }

            TituloSeccion("Historial (${ubicaciones.size})")

            if (ubicaciones.isEmpty()) {
                Text(
                    text = "Todavía no guardas ninguna ubicación.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TablaDatos(
                    encabezadoIzquierdo = "Cuándo",
                    encabezadoDerecho = "Dónde",
                    filas = ubicaciones
                        .sortedByDescending { it.registradaEn }
                        .map { FORMATO_FECHA.format(Date(it.registradaEn)) to it.coordenadas }
                )

                BotonSecundario(
                    texto = "Borrar el historial",
                    icono = Icons.Filled.DeleteSweep,
                    onClick = {
                        onBorrarHistorial()
                        Vibrador.confirmar(context)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun BuscarDispositivoPreview() {
    VozinhaTheme {
        BuscarDispositivoScreen(
            ubicaciones = listOf(
                Ubicacion("1", -33.4489, -70.6693, 12f, System.currentTimeMillis(), "")
            ),
            ocupado = false,
            onGuardarUbicacion = { _, listo -> listo(null) },
            onBorrarHistorial = {},
            onVolver = {}
        )
    }
}
