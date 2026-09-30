package com.vozinha.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vozinha.app.data.Mensaje
import com.vozinha.app.ui.components.BarraSuperior
import com.vozinha.app.ui.components.BotonPrimario
import com.vozinha.app.ui.components.BotonSecundario
import com.vozinha.app.ui.components.ColumnaFormulario
import com.vozinha.app.ui.components.TituloSeccion
import com.vozinha.app.ui.components.Vibrador
import com.vozinha.app.ui.components.recordarSintetizadorVoz
import com.vozinha.app.ui.theme.VozinhaTheme

/**
 * View Hablar: el teléfono dice en voz alta lo que la persona eligió.
 *
 * Los mensajes guardados aparecen primero, ordenados con los favoritos
 * arriba, porque en una conversación real no hay tiempo para buscar. Cada
 * reproducción se confirma en pantalla y con vibración, nunca con sonido.
 */
@Composable
fun HablarScreen(
    mensajes: List<Mensaje>,
    anchoPantalla: WindowWidthSizeClass,
    onRegistrarReproduccion: (String) -> Unit,
    onVolver: () -> Unit
) {
    var libre by rememberSaveable { mutableStateOf("") }
    var ultimoDicho by rememberSaveable { mutableStateOf<String?>(null) }

    val sintetizador = recordarSintetizadorVoz()
    val context = LocalContext.current
    val columnas = if (anchoPantalla == WindowWidthSizeClass.Compact) 1 else 2

    // Los favoritos quedan arriba y, dentro de cada grupo, los más recientes.
    val ordenados = mensajes.sortedWith(
        compareByDescending<Mensaje> { it.favorito }.thenByDescending { it.creadoEn }
    )

    fun decir(texto: String, id: String?) {
        if (sintetizador.hablar(texto)) {
            ultimoDicho = texto
            id?.let(onRegistrarReproduccion)
            Vibrador.confirmar(context)
        } else {
            Vibrador.alertar(context)
        }
    }

    Scaffold(topBar = { BarraSuperior("Hablar", onVolver) }) { padding ->
        ColumnaFormulario(padding) {
            EstadoDelMotor(
                listo = sintetizador.listo,
                disponible = sintetizador.disponible,
                idiomaDisponible = sintetizador.idiomaDisponible,
                hablando = sintetizador.hablando,
                ultimoError = sintetizador.ultimoError
            )

            AnimatedVisibility(visible = ultimoDicho != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Se dijo en voz alta",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = ultimoDicho.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            TituloSeccion("Toca un mensaje para decirlo")

            if (ordenados.isEmpty()) {
                Text(
                    text = "Todavía no tienes mensajes. Guárdalos desde Escribir.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ordenados.chunked(columnas).forEach { fila ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    fila.forEach { mensaje ->
                        Card(
                            onClick = { if (sintetizador.listo) decir(mensaje.texto, mensaje.id) },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 64.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                if (mensaje.favorito) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Favorito",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = mensaje.texto,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    repeat(columnas - fila.size) { Box(Modifier.weight(1f)) }
                }
            }

            TituloSeccion("O escribe algo puntual")

            OutlinedTextField(
                value = libre,
                onValueChange = { libre = it },
                label = { Text("Decir una sola vez") },
                placeholder = { Text("Esto no se guarda") },
                minLines = 2,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
            )

            BotonPrimario(
                texto = "Decirlo en voz alta",
                icono = Icons.Filled.RecordVoiceOver,
                habilitado = sintetizador.listo && libre.isNotBlank(),
                onClick = { decir(libre, null) }
            )

            BotonSecundario(
                texto = "Detener",
                icono = Icons.Filled.Stop,
                onClick = {
                    sintetizador.detener()
                    Vibrador.confirmar(context)
                }
            )
        }
    }
}

/**
 * Tarjeta con el estado del motor de voz.
 *
 * La persona usuaria no puede oír si el motor funcionó, así que el estado y el
 * detalle del último fallo tienen que estar escritos en pantalla.
 */
@Composable
private fun EstadoDelMotor(
    listo: Boolean,
    disponible: Boolean,
    idiomaDisponible: Boolean,
    hablando: Boolean,
    ultimoError: String?
) {
    val (icono, texto, color) = when {
        !disponible -> Triple(
            Icons.Filled.Warning,
            "Tu teléfono no tiene el motor de voz instalado",
            MaterialTheme.colorScheme.errorContainer
        )

        !idiomaDisponible -> Triple(
            Icons.Filled.Warning,
            "Falta la voz en español. Instálala desde los ajustes del teléfono.",
            MaterialTheme.colorScheme.errorContainer
        )

        hablando -> Triple(
            Icons.Filled.RecordVoiceOver,
            "Hablando ahora",
            MaterialTheme.colorScheme.secondaryContainer
        )

        listo -> Triple(
            Icons.Filled.CheckCircle,
            "Listo para hablar por ti",
            MaterialTheme.colorScheme.primaryContainer
        )

        else -> Triple(
            Icons.Filled.Warning,
            "Preparando el motor de voz",
            MaterialTheme.colorScheme.secondaryContainer
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(26.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = texto, style = MaterialTheme.typography.titleMedium)
                if (ultimoError != null) {
                    Text(
                        text = "Detalle del último fallo: $ultimoError",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HablarPreview() {
    VozinhaTheme {
        HablarScreen(
            mensajes = listOf(
                Mensaje("1", "Necesito ayuda, por favor", 2L, 3, true),
                Mensaje("2", "¿Me puedes escribir lo que dijiste?", 1L, 0, false)
            ),
            anchoPantalla = WindowWidthSizeClass.Compact,
            onRegistrarReproduccion = {},
            onVolver = {}
        )
    }
}
