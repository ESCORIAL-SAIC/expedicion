package com.expedicion.app.ui.config

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expedicion.app.ui.common.ExpScaffold
import com.expedicion.app.ui.common.versionesTexto
import com.expedicion.app.util.AppRestarter

/**
 * Pantalla de configuración de la URL base del API.
 *
 * En el primer arranque (sin URL guardada) aparece antes de Login y bloquea la navegación hacia
 * atrás: no se puede seguir sin guardar una URL válida. También es accesible en cualquier momento
 * vía el ícono de ajustes, tanto desde Login como desde el Menú ya logueado; ahí sí se puede
 * cancelar y volver.
 *
 * Ojo al entrar desde el Menú: guardar una URL nueva reinicia el proceso (ver [AppRestarter]) y
 * `SessionManager` sólo vive en memoria, así que se pierde la sesión y hay que volver a loguearse.
 * Cancelar no reinicia nada y deja la sesión intacta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigScreen(
    onCancelar: () -> Unit = {},
    viewModel: ApiConfigViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(enabled = uiState.isInitialSetup) {
        // No hay a donde volver: sin URL configurada no existe pantalla anterior válida.
    }

    // El reinicio del proceso (necesario para que Hilt reconstruya Retrofit con la URL nueva, ver
    // AppRestarter) se dispara sólo cuando el usuario acepta el aviso de conexión verificada: si se
    // reiniciara automáticamente al guardar, el mensaje con la versión del API no llegaría a verse.
    if (uiState.guardadoExitoso && uiState.verificacionOk != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Conexión verificada") },
            text = { Text(uiState.verificacionOk!!) },
            confirmButton = {
                TextButton(onClick = { AppRestarter.restart(context) }) { Text("Continuar") }
            },
        )
    }

    ExpScaffold(
        titulo = "Configuración del servidor",
        onBack = if (uiState.isInitialSetup) null else onCancelar,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                return@Column
            }

            Text(
                text = if (uiState.isInitialSetup) {
                    "Ingrese la dirección del servidor para poder continuar. " +
                        "Se verificará la conexión antes de guardarla."
                } else {
                    "Ingrese la nueva dirección del servidor. " +
                        "Se verificará la conexión antes de guardarla."
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            OutlinedTextField(
                value = uiState.urlInput,
                onValueChange = viewModel::onUrlChange,
                label = { Text("URL del servidor") },
                singleLine = true,
                isError = uiState.errorMessage != null,
                supportingText = uiState.errorMessage?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.guardar() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )

            Button(
                onClick = viewModel::guardar,
                enabled = !uiState.isVerificando,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 12.dp),
            ) {
                Text(if (uiState.isVerificando) "Verificando conexión…" else "Verificar y guardar")
            }

            if (!uiState.isInitialSetup) {
                OutlinedButton(
                    onClick = onCancelar,
                    enabled = !uiState.isVerificando,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("Cancelar")
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = versionesTexto(appVersion = uiState.appVersion, apiVersion = uiState.apiVersion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
