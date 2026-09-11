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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vozinha.app.data.FRASES_INICIALES
import com.vozinha.app.data.Usuario
import com.vozinha.app.data.UsuariosRepository
import com.vozinha.app.ui.components.BarraSuperior
import com.vozinha.app.ui.components.BotonPrimario
import com.vozinha.app.ui.components.BotonSecundario
import com.vozinha.app.ui.components.ColumnaFormulario
import com.vozinha.app.ui.components.TablaDatos
import com.vozinha.app.ui.components.TituloSeccion
import com.vozinha.app.ui.components.Vibrador
import com.vozinha.app.ui.components.recordarSintetizadorVoz
import com.vozinha.app.ui.theme.VozinhaTheme
import com.vozinha.app.util.resumen
import kotlinx.coroutines.launch

/**
 * View principal de comunicación.
 *
 * La persona escribe y el teléfono habla por ella. Como no puede oír el
 * resultado, cada envío se confirma con un aviso visible y una vibración.
 */
@Composable
fun HomeScreen(
    usuario: Usuario,
    frases: List<String>,
    anchoPantalla: WindowWidthSizeClass,
    onAgregarFrase: (String) -> Boolean,
    onEliminarFrase: (String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    var mensaje by rememberSaveable { mutableStateOf("") }
    var ultimoDicho by rememberSaveable { mutableStateOf<String?>(null) }
    var fraseNueva by rememberSaveable { mutableStateOf("") }
    var fraseAEliminar by rememberSaveable { mutableStateOf<String?>(null) }

    val sintetizador = recordarSintetizadorVoz()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val columnas = if (anchoPantalla == WindowWidthSizeClass.Compact) 1 else 2

    fun decir(texto: String) {
        if (sintetizador.hablar(texto)) {
            ultimoDicho = texto.trim()
            Vibrador.confirmar(context)
        } else {
            Vibrador.alertar(context)
        }
    }

    Scaffold(
        topBar = {
            BarraSuperior("Hola, ${usuario.nombre.substringBefore(' ')}") {
                IconButton(onClick = onCerrarSesion) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Cerrar sesión"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ColumnaFormulario(padding) {
            EstadoDelMotor(
                listo = sintetizador.listo,
                disponible = sintetizador.disponible,
                idiomaDisponible = sintetizador.idiomaDisponible,
                hablando = sintetizador.hablando,
                ultimoError = sintetizador.ultimoError
            )

            TituloSeccion("Escribe lo que quieres decir")

            OutlinedTextField(
                value = mensaje,
                onValueChange = { mensaje = it },
                label = { Text("Tu mensaje") },
                placeholder = { Text("Por ejemplo: necesito ayuda") },
                minLines = 3,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
            )

            BotonPrimario(
                texto = "Decirlo en voz alta",
                icono = Icons.Filled.RecordVoiceOver,
                habilitado = sintetizador.listo && mensaje.isNotBlank(),
                onClick = { decir(mensaje) }
            )

            BotonSecundario(
                texto = "Detener",
                icono = Icons.Filled.Stop,
                onClick = {
                    sintetizador.detener()
                    Vibrador.confirmar(context)
                }
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

            TituloSeccion("Frases rápidas")

            if (frases.isEmpty()) {
                Text(
                    text = "No te quedan frases guardadas. Agrega una abajo.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            frases.chunked(columnas).forEach { fila ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    fila.forEach { frase ->
                        Card(
                            onClick = { if (sintetizador.listo) decir(frase) },
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
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(start = 14.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    text = frase,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { fraseAEliminar = frase }) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Eliminar la frase: $frase",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    repeat(columnas - fila.size) { Box(modifier = Modifier.weight(1f)) }
                }
            }

            OutlinedTextField(
                value = fraseNueva,
                onValueChange = { fraseNueva = it },
                label = { Text("Nueva frase") },
                placeholder = { Text("Por ejemplo: ¿me puedes repetir?") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
            )

            BotonSecundario(
                texto = "Agregar frase",
                icono = Icons.Filled.Add,
                onClick = {
                    if (onAgregarFrase(fraseNueva)) {
                        fraseNueva = ""
                        Vibrador.confirmar(context)
                    } else {
                        Vibrador.alertar(context)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Escribe una frase que no esté repetida."
                            )
                        }
                    }
                }
            )

            TituloSeccion("Tu perfil")

            TablaDatos(
                encabezadoIzquierdo = "Dato",
                encabezadoDerecho = "Valor",
                filas = listOf(
                    "Quién eres" to usuario.resumen,
                    "Correo" to usuario.correo,
                    "Comunicación" to usuario.medioPreferido.etiqueta
                )
            )
        }
    }

    val frasePendiente = fraseAEliminar
    if (frasePendiente != null) {
        DialogoEliminarFrase(
            frase = frasePendiente,
            onConfirmar = {
                onEliminarFrase(frasePendiente)
                fraseAEliminar = null
                Vibrador.confirmar(context)
            },
            onCancelar = { fraseAEliminar = null }
        )
    }
}

/** Diálogo de confirmación para borrar una frase. */
@Composable
private fun DialogoEliminarFrase(
    frase: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("¿Eliminar esta frase?") },
        text = {
            Text(
                text = "Se quitará de tus frases rápidas:\n\n\"$frase\"",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text("Eliminar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

/**
 * Aviso permanente del estado del motor de voz.
 *
 * Sustituye a la señal sonora que una persona oyente usaría para darse cuenta
 * de que la aplicación está lista.
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
                // El fallo capturado por try/catch se escribe en pantalla,
                // porque quien usa la aplicación no puede oír que algo salió mal.
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
private fun HomePreview() {
    VozinhaTheme {
        HomeScreen(
            usuario = UsuariosRepository.obtenerUsuarios().first(),
            frases = FRASES_INICIALES,
            anchoPantalla = WindowWidthSizeClass.Compact,
            onAgregarFrase = { true },
            onEliminarFrase = {},
            onCerrarSesion = {}
        )
    }
}
