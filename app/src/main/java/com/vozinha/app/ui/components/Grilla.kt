package com.vozinha.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Elemento de la grilla informativa. */
data class ItemInformativo(val icono: ImageVector, val titulo: String, val detalle: String)

/**
 * Grilla de dos columnas construida por filas.
 *
 * El alto lo define el contenido, así ningún texto queda cortado aunque la
 * persona tenga configurada una letra más grande en su teléfono.
 */
@Composable
private fun GrillaDeDosColumnas(
    cantidad: Int,
    modifier: Modifier = Modifier,
    celda: @Composable (Int) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        (0 until cantidad).chunked(2).forEach { fila ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                fila.forEach { indice ->
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) { celda(indice) }
                }
                if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Grilla de tarjetas informativas. */
@Composable
fun GrillaInformativa(items: List<ItemInformativo>, modifier: Modifier = Modifier) {
    GrillaDeDosColumnas(cantidad = items.size, modifier = modifier) { indice ->
        val item = items[indice]
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth().fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = item.icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(text = item.titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = item.detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Tarjeta de opción, usada por las dos grillas de selección. */
@Composable
private fun TarjetaOpcion(
    texto: String,
    marcada: Boolean,
    onClick: () -> Unit,
    control: @Composable () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (marcada) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (marcada) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),
        modifier = Modifier.fillMaxWidth().fillMaxHeight().heightIn(min = 64.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight().padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            control()
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

/**
 * Radio buttons presentados en grilla de dos columnas.
 *
 * Es genérica para poder trabajar con enums en lugar de cadenas de texto: la
 * comparación se hace sobre el valor y no sobre cómo esté escrita la etiqueta.
 */
@Composable
fun <T> GrillaSeleccionUnica(
    opciones: List<T>,
    seleccionada: T,
    etiqueta: (T) -> String,
    onSelecciona: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    GrillaDeDosColumnas(cantidad = opciones.size, modifier = modifier) { indice ->
        val opcion = opciones[indice]
        TarjetaOpcion(
            texto = etiqueta(opcion),
            marcada = opcion == seleccionada,
            onClick = { onSelecciona(opcion) }
        ) {
            RadioButton(selected = opcion == seleccionada, onClick = null)
        }
    }
}
