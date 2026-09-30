package com.vozinha.app.ui.screens

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vozinha.app.data.Usuario
import com.vozinha.app.data.UsuariosRepository
import com.vozinha.app.ui.components.BarraSuperior
import com.vozinha.app.ui.components.ColumnaFormulario
import com.vozinha.app.ui.components.TablaDatos
import com.vozinha.app.ui.components.TituloSeccion
import com.vozinha.app.ui.theme.VozinhaTheme
import com.vozinha.app.util.resumen

/** Una opción del menú principal. */
private data class OpcionMenu(
    val titulo: String,
    val detalle: String,
    val icono: ImageVector,
    val onAbrir: () -> Unit
)

/**
 * View HomeMenú: el punto desde el que se llega a todo lo demás.
 *
 * Presenta una opción por tarjeta, con un título corto, una explicación en
 * lenguaje simple y un ícono grande. Separar el menú de las funciones evita
 * que la persona tenga que desplazarse por una pantalla larga para encontrar
 * lo que necesita en medio de una conversación.
 */
@Composable
fun HomeMenuScreen(
    usuario: Usuario,
    nombreBackend: String,
    totalMensajes: Int,
    anchoPantalla: WindowWidthSizeClass,
    onEscribir: () -> Unit,
    onHablar: () -> Unit,
    onBuscarDispositivo: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val columnas = if (anchoPantalla == WindowWidthSizeClass.Compact) 1 else 2

    val opciones = listOf(
        OpcionMenu(
            titulo = "Escribir",
            detalle = "Escribe lo que quieres decir y guárdalo para usarlo después",
            icono = Icons.Filled.Edit,
            onAbrir = onEscribir
        ),
        OpcionMenu(
            titulo = "Hablar",
            detalle = "El teléfono dice en voz alta tus mensajes guardados",
            icono = Icons.Filled.RecordVoiceOver,
            onAbrir = onHablar
        ),
        OpcionMenu(
            titulo = "Buscar dispositivo",
            detalle = "Muestra dónde estás para que alguien te encuentre",
            icono = Icons.Filled.MyLocation,
            onAbrir = onBuscarDispositivo
        )
    )

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
        }
    ) { padding ->
        ColumnaFormulario(padding) {
            TituloSeccion("¿Qué quieres hacer?")

            opciones.chunked(columnas).forEach { fila ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    fila.forEach { opcion ->
                        TarjetaOpcionMenu(opcion, Modifier.weight(1f))
                    }
                    repeat(columnas - fila.size) { Box(Modifier.weight(1f)) }
                }
            }

            TituloSeccion("Tu perfil")

            TablaDatos(
                encabezadoIzquierdo = "Dato",
                encabezadoDerecho = "Valor",
                filas = listOf(
                    "Quién eres" to usuario.resumen,
                    "Correo" to usuario.correo,
                    "Comunicación" to usuario.medioPreferido.etiqueta,
                    "Mensajes guardados" to "$totalMensajes",
                    "Datos guardados en" to nombreBackend
                )
            )
        }
    }
}

@Composable
private fun TarjetaOpcionMenu(opcion: OpcionMenu, modifier: Modifier = Modifier) {
    Card(
        onClick = opcion.onAbrir,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = 108.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Icon(
                imageVector = opcion.icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = opcion.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = opcion.detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeMenuPreview() {
    VozinhaTheme {
        HomeMenuScreen(
            usuario = UsuariosRepository.obtenerUsuarios().first(),
            nombreBackend = "Firebase",
            totalMensajes = 4,
            anchoPantalla = WindowWidthSizeClass.Compact,
            onEscribir = {},
            onHablar = {},
            onBuscarDispositivo = {},
            onCerrarSesion = {}
        )
    }
}
