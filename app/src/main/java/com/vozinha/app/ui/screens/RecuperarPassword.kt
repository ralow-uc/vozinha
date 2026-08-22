package com.vozinha.app.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vozinha.app.ui.components.BarraSuperior
import com.vozinha.app.ui.components.BotonPrimario
import com.vozinha.app.ui.components.CampoTexto
import com.vozinha.app.ui.components.ColumnaFormulario
import com.vozinha.app.ui.components.GrillaSeleccionUnica
import com.vozinha.app.ui.components.TablaDatos
import com.vozinha.app.ui.components.TituloSeccion
import com.vozinha.app.ui.components.Vibrador
import com.vozinha.app.ui.components.Vinculo
import com.vozinha.app.ui.theme.VozinhaTheme
import com.vozinha.app.util.Validaciones

/** Formas de recibir el enlace de recuperación. */
private val MEDIOS_ENVIO = listOf("Por correo electrónico", "Por mensaje de texto")

/**
 * View de recuperación de contraseña.
 *
 * Comprueba que el correo pertenezca a alguno de los usuarios del arreglo
 * antes de confirmar el envío, que es simulado porque no hay servidor.
 */
@Composable
fun RecuperarPasswordScreen(
    onCorreoRegistrado: (String) -> Boolean,
    onVolver: () -> Unit
) {
    var correo by rememberSaveable { mutableStateOf("") }
    var medioElegido by rememberSaveable { mutableStateOf(MEDIOS_ENVIO.first()) }
    var intentoEnvio by rememberSaveable { mutableStateOf(false) }
    var correoDesconocido by rememberSaveable { mutableStateOf(false) }
    var mostrarConfirmacion by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(topBar = { BarraSuperior("Recuperar contraseña", onVolver) }) { padding ->
        ColumnaFormulario(padding) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No te preocupes. Escribe tu correo y te enviaremos los pasos " +
                        "para crear una contraseña nueva.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }

            CampoTexto(
                valor = correo,
                onValorCambia = {
                    correo = it
                    correoDesconocido = false
                },
                etiqueta = "Correo electrónico",
                icono = Icons.Filled.Email,
                ayuda = "El mismo correo con el que creaste tu cuenta",
                error = when {
                    correoDesconocido -> "Ese correo no está registrado en Vozinha"
                    intentoEnvio -> Validaciones.errorEmail(correo)
                    else -> null
                },
                tecladoTipo = KeyboardType.Email,
                imeAction = ImeAction.Done
            )

            TituloSeccion("¿Cómo quieres recibir las instrucciones?")
            GrillaSeleccionUnica(
                opciones = MEDIOS_ENVIO,
                seleccionada = medioElegido,
                etiqueta = { it },
                onSelecciona = { medioElegido = it }
            )

            TituloSeccion("Qué va a pasar")
            TablaDatos(
                encabezadoIzquierdo = "Paso",
                encabezadoDerecho = "Qué ocurre",
                filas = listOf(
                    "1" to "Revisas tu correo",
                    "2" to "Abres el enlace que te enviamos",
                    "3" to "Escribes tu contraseña nueva",
                    "4" to "Ingresas con la contraseña nueva"
                )
            )

            BotonPrimario(
                texto = "Enviar instrucciones",
                icono = Icons.AutoMirrored.Filled.Send,
                onClick = {
                    intentoEnvio = true
                    correoDesconocido = false
                    when {
                        Validaciones.errorEmail(correo) != null -> Vibrador.alertar(context)
                        onCorreoRegistrado(correo) -> {
                            Vibrador.confirmar(context)
                            mostrarConfirmacion = true
                        }

                        else -> {
                            correoDesconocido = true
                            Vibrador.alertar(context)
                        }
                    }
                }
            )

            Vinculo("Volver al inicio de sesión", onVolver, Modifier.fillMaxWidth())
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = { Text("Instrucciones enviadas") },
            text = {
                Text(
                    text = "Enviamos los pasos a ${correo.trim()} " +
                        "(${medioElegido.lowercase()}). Revisa tu bandeja de entrada.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarConfirmacion = false
                        onVolver()
                    }
                ) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RecuperarPreview() {
    VozinhaTheme { RecuperarPasswordScreen({ true }, {}) }
}
