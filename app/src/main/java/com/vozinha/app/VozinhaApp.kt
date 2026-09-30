package com.vozinha.app

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vozinha.app.ui.AppViewModel
import com.vozinha.app.ui.screens.BuscarDispositivoScreen
import com.vozinha.app.ui.screens.EscribirScreen
import com.vozinha.app.ui.screens.HablarScreen
import com.vozinha.app.ui.screens.HomeMenuScreen
import com.vozinha.app.ui.screens.LoginScreen
import com.vozinha.app.ui.screens.RecuperarPasswordScreen
import com.vozinha.app.ui.screens.RegistroScreen

/** Rutas de navegación de la aplicación. */
object Rutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val RECUPERAR = "recuperar"
    const val HOME_MENU = "homeMenu"
    const val ESCRIBIR = "escribir"
    const val HABLAR = "hablar"
    const val BUSCAR_DISPOSITIVO = "buscarDispositivo"
}

/**
 * Grafo de navegación de las siete views.
 *
 * El recorrido tiene dos zonas. La de acceso reúne Login, Registro y Recuperar
 * contraseña; al entrar, el login sale de la pila para que el botón atrás no
 * devuelva al formulario. La zona con sesión abierta parte en HomeMenú y desde
 * ahí se llega a Escribir, Hablar y Buscar dispositivo, que siempre vuelven al
 * menú: la persona nunca queda sin saber cómo salir de una pantalla.
 *
 * Las views reciben funciones de navegación en lugar del NavHostController,
 * así se pueden previsualizar y probar por separado.
 */
@Composable
fun AppNavGraph(
    anchoPantalla: WindowWidthSizeClass,
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = viewModel()
) {
    NavHost(navController = navController, startDestination = Rutas.LOGIN) {

        composable(Rutas.LOGIN) {
            LoginScreen(
                usuarios = appViewModel.usuarios,
                ocupado = appViewModel.ocupado,
                onIngresar = { correo, password, onListo ->
                    appViewModel.iniciarSesion(correo, password) { error ->
                        if (error == null) {
                            navController.navigate(Rutas.HOME_MENU) {
                                popUpTo(Rutas.LOGIN) { inclusive = true }
                            }
                        }
                        onListo(error)
                    }
                },
                onIrARegistro = { navController.navigate(Rutas.REGISTRO) },
                onIrARecuperar = { navController.navigate(Rutas.RECUPERAR) }
            )
        }

        composable(Rutas.REGISTRO) {
            RegistroScreen(
                totalUsuarios = appViewModel.totalUsuarios,
                ocupado = appViewModel.ocupado,
                onCorreoRegistrado = { correo -> appViewModel.correoRegistrado(correo) },
                onRegistrar = { usuario, onListo -> appViewModel.registrar(usuario, onListo) },
                onRegistroCompleto = { navController.popBackStack() },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Rutas.RECUPERAR) {
            RecuperarPasswordScreen(
                ocupado = appViewModel.ocupado,
                onCorreoRegistrado = { correo -> appViewModel.correoRegistrado(correo) },
                onRecuperar = { correo, onListo -> appViewModel.recuperarPassword(correo, onListo) },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Rutas.HOME_MENU) {
            val usuario = appViewModel.sesionActiva
            if (usuario == null) {
                // La navegación va dentro de un LaunchedEffect y no en el cuerpo
                // del composable: llamarla durante la composición la repetiría
                // en cada recomposición y apilaría destinos.
                LaunchedEffect(Unit) { volverAlLogin(navController) }
            } else {
                HomeMenuScreen(
                    usuario = usuario,
                    nombreBackend = appViewModel.nombreBackend,
                    totalMensajes = appViewModel.mensajes.size,
                    anchoPantalla = anchoPantalla,
                    onEscribir = { navController.navigate(Rutas.ESCRIBIR) },
                    onHablar = { navController.navigate(Rutas.HABLAR) },
                    onBuscarDispositivo = { navController.navigate(Rutas.BUSCAR_DISPOSITIVO) },
                    onCerrarSesion = {
                        appViewModel.cerrarSesion()
                        volverAlLogin(navController)
                    }
                )
            }
        }

        composable(Rutas.ESCRIBIR) {
            EscribirScreen(
                mensajes = appViewModel.mensajes,
                ocupado = appViewModel.ocupado,
                onGuardar = { texto, onListo -> appViewModel.guardarMensaje(texto, onListo) },
                onCambiarFavorito = { id -> appViewModel.cambiarFavorito(id) },
                onEliminar = { id -> appViewModel.eliminarMensaje(id) },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Rutas.HABLAR) {
            HablarScreen(
                mensajes = appViewModel.mensajes,
                anchoPantalla = anchoPantalla,
                onRegistrarReproduccion = { id -> appViewModel.registrarReproduccion(id) },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Rutas.BUSCAR_DISPOSITIVO) {
            BuscarDispositivoScreen(
                ubicaciones = appViewModel.ubicaciones,
                ocupado = appViewModel.ocupado,
                onGuardarUbicacion = { ubicacion, onListo ->
                    appViewModel.guardarUbicacion(ubicacion, onListo)
                },
                onBorrarHistorial = { appViewModel.borrarHistorialDeUbicaciones() },
                onVolver = { navController.popBackStack() }
            )
        }
    }
}

/** Deja el login como único destino de la pila. */
private fun volverAlLogin(navController: NavHostController) {
    navController.navigate(Rutas.LOGIN) {
        popUpTo(0) { inclusive = true }
    }
}
