package com.vozinha.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografía con tamaños mayores a los valores por omisión de Material 3.
 *
 * La aplicación está dirigida a personas con discapacidad auditiva, que
 * dependen del canal visual: el texto es el medio principal de comunicación y
 * necesita leerse sin esfuerzo.
 */
private val Tipografia = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp)
)

// El azul profundo sobre fondo claro da un contraste muy alto
//necesario porque toda la retroalimentación de la aplicación es visual.
private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF1B4F91),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FF),
    onPrimaryContainer = Color(0xFF0A1F3D),
    secondary = Color(0xFF8A5000),
    secondaryContainer = Color(0xFFFFDDB0),
    onSecondaryContainer = Color(0xFF2C1700),
    background = Color(0xFFF9FAFC),
    onBackground = Color(0xFF141A22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141A22),
    surfaceVariant = Color(0xFFE6EAF1),
    onSurfaceVariant = Color(0xFF3F4854),
    outline = Color(0xFF6F7885),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC)
)

private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    onPrimary = Color(0xFF0A1F3D),
    primaryContainer = Color(0xFF123564),
    onPrimaryContainer = Color(0xFFD3E3FF),
    secondary = Color(0xFFFFB95C),
    secondaryContainer = Color(0xFF5F3800),
    onSecondaryContainer = Color(0xFFFFDDB0),
    background = Color(0xFF0F1319),
    onBackground = Color(0xFFE6E9EF),
    surface = Color(0xFF1A1F27),
    onSurface = Color(0xFFE6E9EF),
    surfaceVariant = Color(0xFF1A1F27),
    onSurfaceVariant = Color(0xFF8F98A6),
    outline = Color(0xFF8F98A6),
    error = Color(0xFFF2B8B5)
)

/** Tema de la aplicación. Sigue el modo claro u oscuro del sistema. */
@Composable
fun VozinhaTheme(
    modoOscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (modoOscuro) EsquemaOscuro else EsquemaClaro,
        typography = Tipografia,
        content = content
    )
}
