package com.vozinha.app.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vozinha.app.data.MedioComunicacion
import com.vozinha.app.data.TipoUsuario
import com.vozinha.app.data.Usuario
import com.vozinha.app.ui.ResultadoRegistro
import com.vozinha.app.ui.components.BarraSuperior
import com.vozinha.app.ui.components.BotonPrimario
import com.vozinha.app.ui.components.CampoTexto
import com.vozinha.app.ui.components.ColumnaFormulario
import com.vozinha.app.ui.components.FilaCheck
import com.vozinha.app.ui.components.GrillaSeleccionUnica
import com.vozinha.app.ui.components.TablaDatos
import com.vozinha.app.ui.components.TituloSeccion
import com.vozinha.app.ui.components.Vibrador
import com.vozinha.app.ui.components.Vinculo
import com.vozinha.app.ui.theme.VozinhaTheme
import com.vozinha.app.util.ExigenciaCorreo
import com.vozinha.app.util.Validaciones
import com.vozinha.app.util.normalizado
import com.vozinha.app.util.primerError
import com.vozinha.app.util.reglaDeCorreo
import kotlinx.coroutines.launch

/**
 * View de registro de usuario.
 *
 * Es la que alimenta el arreglo: cada registro válido agrega un usuario con su
 * contraseña. Reúne el combo box, los radio buttons y el check list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    totalUsuarios: Int,
    ocupado: Boolean,
    onCorreoRegistrado: (String) -> Boolean,
    onRegistrar: (Usuario, (ResultadoRegistro) -> Unit) -> Unit,
    onRegistroCompleto: () -> Unit,
    onVolver: () -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmacion by rememberSaveable { mutableStateOf("") }
    var tipoUsuario by rememberSaveable { mutableStateOf(TipoUsuario.PERSONA_SORDA) }
    var comboAbierto by remember { mutableStateOf(false) }
    var medioPreferido by rememberSaveable { mutableStateOf(MedioComunicacion.TEXTO_ESCRITO) }
    var aceptaTerminos by rememberSaveable { mutableStateOf(false) }
    var intentoEnvio by rememberSaveable { mutableStateOf(false) }
    var mostrarExito by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun avisar(mensaje: String) {
        Vibrador.alertar(context)
        scope.launch { snackbarHostState.showSnackbar(mensaje) }
    }

    // Aquí el correo, además de tener formato válido, tiene que estar libre.
    // reglaDeCorreo devuelve esa regla ya armada y la view solo la aplica.
    val reglaCorreo = reglaDeCorreo(ExigenciaCorreo.DEBE_SER_NUEVO, onCorreoRegistrado)

    Scaffold(
        topBar = { BarraSuperior("Crear cuenta", onVolver) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ColumnaFormulario(padding) {
            Text(
                text = "Completa tus datos para crear tu cuenta en Vozinha.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TituloSeccion("Tus datos")

            CampoTexto(
                valor = nombre,
                onValorCambia = { nombre = it },
                etiqueta = "Nombre",
                icono = Icons.Filled.Person,
                error = if (intentoEnvio) Validaciones.errorNombre(nombre) else null
            )

            CampoTexto(
                valor = correo,
                onValorCambia = { correo = it },
                etiqueta = "Correo electrónico",
                icono = Icons.Filled.Email,
                ayuda = "Lo usarás para ingresar a la aplicación",
                error = if (intentoEnvio) reglaCorreo(correo) else null,
                tecladoTipo = KeyboardType.Email
            )

            CampoTexto(
                valor = password,
                onValorCambia = { password = it },
                etiqueta = "Contraseña",
                icono = Icons.Filled.Lock,
                ayuda = "Mínimo ${Validaciones.LARGO_MINIMO_PASSWORD} caracteres",
                error = if (intentoEnvio) Validaciones.errorPassword(password) else null,
                esPassword = true
            )

            CampoTexto(
                valor = confirmacion,
                onValorCambia = { confirmacion = it },
                etiqueta = "Repetir contraseña",
                icono = Icons.Filled.Lock,
                error = if (intentoEnvio) {
                    Validaciones.errorConfirmacion(password, confirmacion)
                } else {
                    null
                },
                esPassword = true,
                imeAction = ImeAction.Done
            )

            TituloSeccion("Cuéntanos sobre ti")

            ExposedDropdownMenuBox(
                expanded = comboAbierto,
                onExpandedChange = { comboAbierto = it }
            ) {
                OutlinedTextField(
                    value = tipoUsuario.etiqueta,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de usuario") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = comboAbierto)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 68.dp)
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = comboAbierto,
                    onDismissRequest = { comboAbierto = false }
                ) {
                    TipoUsuario.entries.forEach { opcion ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = opcion.etiqueta,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            onClick = {
                                tipoUsuario = opcion
                                comboAbierto = false
                            }
                        )
                    }
                }
            }

            TituloSeccion("¿Cómo prefieres comunicarte?")
            GrillaSeleccionUnica(
                opciones = MedioComunicacion.entries,
                seleccionada = medioPreferido,
                etiqueta = { it.etiqueta },
                onSelecciona = { medioPreferido = it }
            )

            FilaCheck("Acepto los términos y condiciones", aceptaTerminos) { aceptaTerminos = it }

            TituloSeccion("Resumen de tu registro")
            TablaDatos(
                encabezadoIzquierdo = "Dato",
                encabezadoDerecho = "Lo que ingresaste",
                filas = listOf(
                    "Nombre" to nombre.ifBlank { "Sin completar" },
                    "Correo" to correo.ifBlank { "Sin completar" },
                    "Tipo de usuario" to tipoUsuario.etiqueta,
                    "Comunicación" to medioPreferido.etiqueta,
                    "Usuarios registrados" to "$totalUsuarios"
                )
            )

            BotonPrimario(
                texto = if (ocupado) "Creando la cuenta..." else "Crear mi cuenta",
                habilitado = !ocupado,
                icono = Icons.Filled.PersonAdd,
                onClick = {
                    intentoEnvio = true
                    // Cada regla se evalúa solo si las anteriores pasaron, de
                    // modo que el aviso nombre el problema real y no un
                    // genérico "revisa los campos".
                    val error = primerError(
                        { Validaciones.errorNombre(nombre) },
                        { reglaCorreo(correo) },
                        { Validaciones.errorPassword(password) },
                        { Validaciones.errorConfirmacion(password, confirmacion) },
                        { if (aceptaTerminos) null else "Debes aceptar los términos para continuar" }
                    )
                    when {
                        error != null -> avisar(error)
                        else -> {
                            val nuevo = Usuario(
                                nombre = nombre.normalizado(),
                                correo = correo.normalizado(),
                                password = password,
                                tipoUsuario = tipoUsuario,
                                medioPreferido = medioPreferido
                            )
                            onRegistrar(nuevo) { resultado ->
                                when (resultado) {
                                    is ResultadoRegistro.Exitoso -> {
                                        Vibrador.confirmar(context)
                                        mostrarExito = true
                                    }

                                    is ResultadoRegistro.Rechazado -> avisar(resultado.motivo)
                                }
                            }
                        }
                    }
                }
            )

            Vinculo("Ya tengo una cuenta, volver al inicio", onVolver, Modifier.fillMaxWidth())
        }
    }

    if (mostrarExito) {
        AlertDialog(
            onDismissRequest = { mostrarExito = false },
            title = { Text("Cuenta creada") },
            text = {
                Text(
                    text = "Listo ${nombre.normalizado()}, tu cuenta quedó registrada. " +
                        "Ya puedes ingresar con ${correo.normalizado()} y tu contraseña.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarExito = false
                        onRegistroCompleto()
                    }
                ) {
                    Text("Ir al inicio de sesión")
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegistroPreview() {
    VozinhaTheme {
        RegistroScreen(
            totalUsuarios = 5,
            ocupado = false,
            onCorreoRegistrado = { false },
            onRegistrar = { _, listo -> listo(ResultadoRegistro.Exitoso(6)) },
            onRegistroCompleto = {},
            onVolver = {}
        )
    }
}
