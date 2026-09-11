package com.vozinha.app

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vozinha.app.ui.AppViewModel
import com.vozinha.app.ui.screens.HomeScreen
import com.vozinha.app.ui.screens.LoginScreen
import com.vozinha.app.ui.screens.RecuperarPasswordScreen
import com.vozinha.app.ui.screens.RegistroScreen

/** Rutas de navegación de la aplicación. */
object Rutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val RECUPERAR = "recuperar"
    const val HOME = "home"
}

/**
 * Grafo de navegación.
 *
 * Las views reciben funciones de navegación en lugar del NavHostController,
 * así se pueden previsualizar por separado. El arreglo de usuarios vive en un
 * ViewModel compartido, para que lo registrado en una view esté disponible en
 * las otras.
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
                onIngresar = { correo, password ->
                    val ingreso = appViewModel.iniciarSesion(correo, password)
                    if (ingreso) {
                        navController.navigate(Rutas.HOME) {
                            popUpTo(Rutas.LOGIN) { inclusive = true }
                        }
                    }
                    ingreso
                },
                onIrARegistro = { navController.navigate(Rutas.REGISTRO) },
                onIrARecuperar = { navController.navigate(Rutas.RECUPERAR) }
            )
        }

        composable(Rutas.REGISTRO) {
            RegistroScreen(
                totalUsuarios = appViewModel.totalUsuarios,
                onCorreoRegistrado = { correo -> appViewModel.correoRegistrado(correo) },
                onRegistrar = { usuario -> appViewModel.registrar(usuario) },
                onRegistroCompleto = { navController.popBackStack() },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Rutas.RECUPERAR) {
            RecuperarPasswordScreen(
                onCorreoRegistrado = { correo -> appViewModel.correoRegistrado(correo) },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Rutas.HOME) {
            val usuario = appViewModel.sesionActiva
            if (usuario == null) {
                // La navegación va dentro de un LaunchedEffect y no en el cuerpo
                // del composable: llamarla durante la composición la repetiría
                // en cada recomposición y apilaría destinos.
                LaunchedEffect(Unit) {
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(Rutas.HOME) { inclusive = true }
                    }
                }
            } else {
                HomeScreen(
                    usuario = usuario,
                    frases = appViewModel.frases,
                    anchoPantalla = anchoPantalla,
                    onAgregarFrase = { texto -> appViewModel.agregarFrase(texto) },
                    onEliminarFrase = { frase -> appViewModel.eliminarFrase(frase) },
                    onCerrarSesion = {
                        appViewModel.cerrarSesion()
                        navController.navigate(Rutas.LOGIN) {
                            popUpTo(Rutas.HOME) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
