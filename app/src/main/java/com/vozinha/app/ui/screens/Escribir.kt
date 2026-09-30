package com.vozinha.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import com.vozinha.app.data.Mensaje
import com.vozinha.app.ui.components.BarraSuperior
import com.vozinha.app.ui.components.BotonPrimario
import com.vozinha.app.ui.components.ColumnaFormulario
import com.vozinha.app.ui.components.TituloSeccion
import com.vozinha.app.ui.components.Vibrador
import com.vozinha.app.ui.theme.VozinhaTheme
import kotlinx.coroutines.launch

/**
 * View Escribir: la persona redacta lo que quiere decir y lo guarda.
 *
 * Separar escribir de hablar permite preparar las frases con calma antes de
 * necesitarlas. Lo que se guarda aquí queda en la base de datos y aparece en
 * la view Hablar, también después de cerrar la aplicación o cambiar de equipo.
 */
@Composable
fun EscribirScreen(
    mensajes: List<Mensaje>,
    ocupado: Boolean,
    onGuardar: (String, (String?) -> Unit) -> Unit,
    onCambiarFavorito: (String) -> Unit,
    onEliminar: (String) -> Unit,
    onVolver: () -> Unit
) {
    var texto by rememberSaveable { mutableStateOf("") }
    var mensajeAEliminar by rememberSaveable { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = { BarraSuperior("Escribir", onVolver) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ColumnaFormulario(padding) {
            Text(
                text = "Escribe una frase y guárdala. Después la vas a encontrar en Hablar.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Tu mensaje") },
                placeholder = { Text("Por ejemplo: necesito ayuda") },
                minLines = 3,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
            )

            BotonPrimario(
                texto = if (ocupado) "Guardando..." else "Guardar mensaje",
                icono = Icons.Filled.Save,
                habilitado = !ocupado && texto.isNotBlank(),
                onClick = {
                    onGuardar(texto) { error ->
                        if (error == null) {
                            texto = ""
                            Vibrador.confirmar(context)
                            scope.launch { snackbarHostState.showSnackbar("Mensaje guardado") }
                        } else {
                            Vibrador.alertar(context)
                            scope.launch { snackbarHostState.showSnackbar(error) }
                        }
                    }
                }
            )

            TituloSeccion("Tus mensajes guardados (${mensajes.size})")

            if (mensajes.isEmpty()) {
                Text(
                    text = "Todavía no guardas ningún mensaje.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            mensajes.forEach { mensaje ->
                FilaMensajeEditable(
                    mensaje = mensaje,
                    onCambiarFavorito = {
                        onCambiarFavorito(mensaje.id)
                        Vibrador.confirmar(context)
                    },
                    onEliminar = { mensajeAEliminar = mensaje.id }
                )
            }
        }
    }

    val pendiente = mensajeAEliminar
    if (pendiente != null) {
        val texto = mensajes.firstOrNull { it.id == pendiente }?.texto.orEmpty()
        AlertDialog(
            onDismissRequest = { mensajeAEliminar = null },
            title = { Text("¿Borrar este mensaje?") },
            text = {
                Text(
                    text = "Se va a borrar: $texto",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEliminar(pendiente)
                        mensajeAEliminar = null
                        Vibrador.confirmar(context)
                    }
                ) { Text("Sí, borrar") }
            },
            dismissButton = {
                TextButton(onClick = { mensajeAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

/** Fila con el mensaje, el botón de favorito y el de borrar. */
@Composable
private fun FilaMensajeEditable(
    mensaje: Mensaje,
    onCambiarFavorito: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = mensaje.texto, style = MaterialTheme.typography.bodyLarge)
                if (mensaje.vecesDicho > 0) {
                    Text(
                        text = "Dicho ${mensaje.vecesDicho} ${if (mensaje.vecesDicho == 1) "vez" else "veces"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onCambiarFavorito) {
                Icon(
                    imageVector = if (mensaje.favorito) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (mensaje.favorito) {
                        "Quitar de favoritos: ${mensaje.texto}"
                    } else {
                        "Marcar como favorito: ${mensaje.texto}"
                    },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Borrar el mensaje: ${mensaje.texto}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EscribirPreview() {
    VozinhaTheme {
        EscribirScreen(
            mensajes = listOf(
                Mensaje("1", "Necesito ayuda, por favor", 0L, 3, true),
                Mensaje("2", "¿Me puedes escribir lo que dijiste?", 0L, 0, false)
            ),
            ocupado = false,
            onGuardar = { _, listo -> listo(null) },
            onCambiarFavorito = {},
            onEliminar = {},
            onVolver = {}
        )
    }
}
