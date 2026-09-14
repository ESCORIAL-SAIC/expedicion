package com.expedicion.app.ui.navigation

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.expedicion.app.ui.config.ApiConfigScreen
import com.expedicion.app.ui.despacho.DespachoScreen
import com.expedicion.app.ui.devolucion.DevolucionScreen
import com.expedicion.app.ui.estado.EstadoScreen
import com.expedicion.app.ui.login.LoginScreen
import com.expedicion.app.ui.menu.MenuScreen

@Composable
fun ExpedicionNavGraph(
    navController: NavHostController = rememberNavController(),
    appStartViewModel: AppStartViewModel = hiltViewModel(),
) {
    val activity = LocalContext.current as? Activity
    val startDestination by appStartViewModel.startDestination.collectAsStateWithLifecycle()

    val resolvedStartDestination = startDestination
    if (resolvedStartDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController = navController, startDestination = resolvedStartDestination) {
        composable(Routes.API_CONFIG) {
            ApiConfigScreen(onCancelar = { navController.popBackStack() })
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MENU) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onExit = { activity?.finish() },
                onAbrirConfiguracion = { navController.navigate(Routes.API_CONFIG) },
            )
        }
        composable(Routes.MENU) {
            MenuScreen(
                onDespacho = { navController.navigate(Routes.DESPACHO) },
                onDevolucion = { navController.navigate(Routes.DEVOLUCION) },
                onEstado = { navController.navigate(Routes.ESTADO) },
                onAbrirConfiguracion = { navController.navigate(Routes.API_CONFIG) },
            )
        }
        composable(Routes.DESPACHO) {
            DespachoScreen(onVolver = { navController.popBackStack() })
        }
        composable(Routes.DEVOLUCION) {
            DevolucionScreen(onVolver = { navController.popBackStack() })
        }
        composable(Routes.ESTADO) {
            EstadoScreen(onVolver = { navController.popBackStack() })
        }
    }
}
