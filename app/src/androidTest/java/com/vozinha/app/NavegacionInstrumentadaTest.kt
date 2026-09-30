package com.vozinha.app

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vozinha.app.ui.theme.VozinhaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas instrumentadas del recorrido entre views.
 *
 * Se ejecutan sobre un dispositivo o emulador con la librería de pruebas de
 * Compose, que se apoya en Espresso para sincronizarse con la interfaz y para
 * el botón físico de retroceso.
 *
 * Verifican lo que las pruebas unitarias no pueden ver: que la persona llegue
 * efectivamente desde el login hasta cada función y pueda devolverse.
 */
@RunWith(AndroidJUnit4::class)
class NavegacionInstrumentadaTest {

    @get:Rule
    val compose = createComposeRule()

    private fun abrirAplicacion() {
        compose.setContent {
            VozinhaTheme {
                AppNavGraph(anchoPantalla = WindowWidthSizeClass.Compact)
            }
        }
    }

    /**
     * Entra con una de las cuentas de prueba y espera a llegar al menú.
     *
     * El texto «Contraseña» aparece dos veces en la view de Login: como
     * etiqueta del campo y como encabezado de la tabla de cuentas de prueba.
     * Por eso el campo se busca exigiendo además que acepte escritura.
     */
    private fun iniciarSesion() {
        abrirAplicacion()
        compose.onNode(hasSetTextAction() and hasText("Correo electrónico"))
            .performTextInput("camila@gmail.com")
        compose.onNode(hasSetTextAction() and hasText("Contraseña"))
            .performTextInput("hola1234")
        compose.onNodeWithText("Ingresar").performScrollTo().performClick()
        // El acceso consulta Firebase, así que la espera contempla la red.
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithTextSeguro("¿Qué quieres hacer?")
        }
    }

    @Test
    fun el_login_muestra_los_accesos_a_las_otras_views() {
        abrirAplicacion()

        compose.onNodeWithText("Vozinha").assertIsDisplayed()
        compose.onNodeWithText("¿Olvidaste tu contraseña?").assertIsDisplayed()
        compose.onNodeWithText("¿No tienes cuenta? Crear una cuenta").assertIsDisplayed()
    }

    @Test
    fun desde_el_login_se_llega_al_registro_y_se_vuelve() {
        abrirAplicacion()

        compose.onNodeWithText("¿No tienes cuenta? Crear una cuenta").performScrollTo().performClick()
        compose.onNodeWithText("Crear cuenta").assertIsDisplayed()

        // El botón físico de retroceso tiene que dejar a la persona en el login.
        Espresso.pressBack()
        compose.onNodeWithText("Ingresar").assertIsDisplayed()
    }

    @Test
    fun desde_el_login_se_llega_a_recuperar_contrasena() {
        abrirAplicacion()

        compose.onNodeWithText("¿Olvidaste tu contraseña?").performScrollTo().performClick()

        compose.onNodeWithText("Recuperar contraseña").assertIsDisplayed()
    }

    @Test
    fun el_menu_lleva_a_escribir_y_permite_volver() {
        iniciarSesion()

        compose.onNodeWithText("Escribir").performClick()
        compose.onNodeWithText("Tu mensaje").assertIsDisplayed()

        Espresso.pressBack()
        compose.onNodeWithText("¿Qué quieres hacer?").assertIsDisplayed()
    }

    @Test
    fun el_menu_lleva_a_hablar_y_a_buscar_dispositivo() {
        iniciarSesion()

        compose.onNodeWithText("Hablar").performClick()
        compose.onNodeWithText("Toca un mensaje para decirlo").assertIsDisplayed()
        Espresso.pressBack()

        compose.onNodeWithText("Buscar dispositivo").performScrollTo().performClick()
        compose.onNodeWithText("Buscar dónde estoy").assertIsDisplayed()
    }

    @Test
    fun un_mensaje_escrito_aparece_en_la_view_hablar() {
        // El texto lleva la marca de tiempo para que la prueba no choque con
        // lo que dejó una ejecución anterior en la base de datos.
        val mensaje = "Necesito un intérprete ${System.currentTimeMillis()}"
        iniciarSesion()

        compose.onNodeWithText("Escribir").performClick()
        compose.onNode(hasSetTextAction() and hasText("Tu mensaje")).performTextInput(mensaje)
        compose.onNodeWithText("Guardar mensaje").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithTextSeguro(mensaje)
        }

        // Se vuelve con la flecha de la barra y no con el botón físico: tras
        // escribir, el retroceso del sistema cierra primero el teclado.
        volverAlMenu()
        compose.onNodeWithText("Hablar").performClick()
        compose.onNodeWithText(mensaje).assertIsDisplayed()

        // La prueba deja la base como la encontró, y de paso cubre el borrado.
        volverAlMenu()
        compose.onNodeWithText("Escribir").performClick()
        compose.onNodeWithContentDescription("Borrar el mensaje: $mensaje").performScrollTo().performClick()
        compose.onNodeWithText("Sí, borrar").performClick()
        compose.waitUntil(timeoutMillis = 20_000) {
            !compose.onAllNodesWithTextSeguro(mensaje)
        }
    }

    /** Vuelve al menú con la flecha de la barra superior. */
    private fun volverAlMenu() {
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTextSeguro("¿Qué quieres hacer?")
        }
    }
}

/** Indica si existe al menos un nodo con ese texto, sin lanzar excepción. */
private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextSeguro(
    texto: String
): Boolean = onAllNodes(
    androidx.compose.ui.test.hasText(texto, substring = true)
).fetchSemanticsNodes().isNotEmpty()
