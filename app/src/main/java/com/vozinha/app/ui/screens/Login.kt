package com.vozinha.app.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.vozinha.app.data.Usuario
import com.vozinha.app.data.UsuariosRepository
import com.vozinha.app.ui.components.BotonPrimario
import com.vozinha.app.ui.components.CampoTexto
import com.vozinha.app.ui.components.ColumnaFormulario
import com.vozinha.app.ui.components.FilaCheck
import com.vozinha.app.ui.components.GrillaInformativa
import com.vozinha.app.ui.components.ItemInformativo
import com.vozinha.app.ui.components.TablaDatos
import com.vozinha.app.ui.components.TituloSeccion
import com.vozinha.app.ui.components.Vibrador
import com.vozinha.app.ui.components.Vinculo
import com.vozinha.app.ui.theme.VozinhaTheme
import com.vozinha.app.util.Validaciones
import kotlinx.coroutines.launch

/** Contenido de la grilla que explica para qué sirve la aplicación. */
private val PRESTACIONES = listOf(
    ItemInformativo(Icons.Filled.RecordVoiceOver, "Escribe y habla", "El teléfono dice en voz alta lo que escribes"),
    ItemInformativo(Icons.Filled.Subtitles, "Frases rápidas", "Las situaciones de siempre, a un toque"),
    ItemInformativo(Icons.Filled.NotificationsActive, "Avisos que se sienten", "Vibración en lugar de sonido"),
    ItemInformativo(Icons.Filled.TextFields, "Todo visible", "Texto grande y alto contraste")
)

/**
 * View de inicio de sesión.
 *
 * Valida el correo y la contraseña contra el arreglo de usuarios registrados.
 */
@Composable
fun LoginScreen(
    usuarios: List<Usuario>,
    onIngresar: (String, String) -> Boolean,
    onIrARegistro: () -> Unit,
    onIrARecuperar: () -> Unit
) {
    var correo by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var recordarCorreo by rememberSaveable { mutableStateOf(false) }
    var intentoEnvio by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        ColumnaFormulario(padding) {
            Text(
                text = "Vozinha",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Tu voz cuando el sonido no alcanza",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            CampoTexto(
                valor = correo,
                onValorCambia = { correo = it },
                etiqueta = "Correo electrónico",
                icono = Icons.Filled.Email,
                ayuda = "Ejemplo: nombre@gmail.com",
                error = if (intentoEnvio) Validaciones.errorEmail(correo) else null,
                tecladoTipo = KeyboardType.Email
            )

            CampoTexto(
                valor = password,
                onValorCambia = { password = it },
                etiqueta = "Contraseña",
                icono = Icons.Filled.Lock,
                error = if (intentoEnvio) Validaciones.errorPassword(password) else null,
                esPassword = true,
                imeAction = ImeAction.Done
            )

            FilaCheck("Recordar mi correo", recordarCorreo) { recordarCorreo = it }

            BotonPrimario(
                texto = "Ingresar",
                icono = Icons.AutoMirrored.Filled.Login,
                onClick = {
                    intentoEnvio = true
                    val completo = Validaciones.errorEmail(correo) == null &&
                        Validaciones.errorPassword(password) == null
                    if (!completo) {
                        Vibrador.alertar(context)
                    } else if (!onIngresar(correo, password)) {
                        Vibrador.alertar(context)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "El correo o la contraseña no coinciden con ninguna cuenta."
                            )
                        }
                    }
                }
            )

            Vinculo("¿Olvidaste tu contraseña?", onIrARecuperar, Modifier.fillMaxWidth())
            Vinculo("¿No tienes cuenta? Crear una cuenta", onIrARegistro, Modifier.fillMaxWidth())

            TituloSeccion("¿Qué hace Vozinha?")
            GrillaInformativa(items = PRESTACIONES)

            TituloSeccion("Cuentas de prueba")
            Text(
                text = "Estas son las ${usuarios.size} cuentas registradas en la aplicación. " +
                    "Puedes entrar con cualquiera de ellas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TablaDatos(
                encabezadoIzquierdo = "Correo",
                encabezadoDerecho = "Contraseña",
                filas = usuarios.map { it.correo to it.password }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginPreview() {
    VozinhaTheme {
        LoginScreen(UsuariosRepository.obtenerUsuarios(), { _, _ -> true }, {}, {})
    }
}
