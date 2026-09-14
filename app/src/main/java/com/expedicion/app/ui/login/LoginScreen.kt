package com.expedicion.app.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expedicion.app.ui.common.ConfirmDialog
import com.expedicion.app.ui.common.ErrorDialog
import com.expedicion.app.ui.common.versionesTexto
import com.expedicion.app.ui.theme.ExpBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onExit: () -> Unit = {},
    onAbrirConfiguracion: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            viewModel.onLoginConsumed()
            onLoginSuccess()
        }
    }

    if (uiState.errorMessage != null) {
        ErrorDialog(message = uiState.errorMessage!!, onDismiss = viewModel::dismissError)
    }

    if (uiState.mostrarConfirmarSalir) {
        ConfirmDialog(
            message = "¿Salir de la aplicación?",
            onConfirm = { viewModel.cancelarSalir(); onExit() },
            onDismiss = viewModel::cancelarSalir,
        )
    }

    // El fondo azul se pinta a pantalla completa (la Activity es edge-to-edge), pero los hijos del
    // Box se corren hacia adentro de las barras del sistema con windowInsetsPadding: sin eso el
    // boton de configuracion queda tapado por la barra de notificaciones. Esta pantalla no usa
    // ExpScaffold, que es quien aplica los insets en el resto de la app.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ExpBlue),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Expedición",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(32.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Ingreso",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )

                    OutlinedTextField(
                        value = uiState.usuario,
                        onValueChange = viewModel::onUsuarioChange,
                        label = { Text("Usuario") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = viewModel::submit,
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text(
                            if (uiState.isLoading) "Ingresando…" else "INGRESAR",
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    OutlinedButton(
                        onClick = viewModel::solicitarSalir,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text("Salir")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = versionesTexto(
                    appVersion = uiState.appVersion,
                    apiVersion = uiState.apiVersion,
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Va declarado DESPUES del Column a proposito: el Column ocupa toda la pantalla y, en un
        // Box, el orden de declaracion es orden de dibujado y de hit-testing. Si el boton se
        // declara antes, el Column queda encima y se come el tap: el icono se ve pero no responde.
        IconButton(
            onClick = onAbrirConfiguracion,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(8.dp),
        ) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Configuración del servidor",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
