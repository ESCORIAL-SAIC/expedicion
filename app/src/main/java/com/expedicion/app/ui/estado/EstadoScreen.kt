package com.expedicion.app.ui.estado

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expedicion.app.data.repository.EstadoOutcome
import com.expedicion.app.ui.common.ErrorDialog
import com.expedicion.app.ui.common.ExpScaffold

private val DisponibleContainer = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadoScreen(
    onVolver: () -> Unit,
    viewModel: EstadoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val etiquetaFocusRequester = remember { FocusRequester() }
    var expandido by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.focusTrigger) {
        etiquetaFocusRequester.requestFocus()
    }

    if (uiState.errorMessage != null) {
        ErrorDialog(message = uiState.errorMessage!!, onDismiss = viewModel::dismissError)
    }

    ExpScaffold(titulo = "Estado", onBack = onVolver) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ExposedDropdownMenuBox(
                expanded = expandido,
                onExpandedChange = { expandido = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                OutlinedTextField(
                    value = uiState.tipo,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
                    TIPOS_DISPONIBLES.forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text(opcion) },
                            onClick = { viewModel.onTipoChange(opcion); expandido = false },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.etiquetaInput,
                onValueChange = viewModel::onEtiquetaChange,
                label = { Text("Etiqueta") },
                singleLine = true,
                enabled = uiState.tipo.isNotBlank(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.consultar() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(etiquetaFocusRequester)
                    .focusable()
                    .padding(bottom = 16.dp),
            )

            when (val resultado = uiState.resultado) {
                is EstadoOutcome.Disponible -> Card(
                    colors = CardDefaults.cardColors(containerColor = DisponibleContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Producto disponible para despachar.", style = MaterialTheme.typography.titleMedium)
                        Text("Producto: ${resultado.productoN}")
                    }
                }
                is EstadoOutcome.NoDisponible -> Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Producto NO disponible para despachar.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text("Cliente: ${resultado.clienteN}", color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Producto: ${resultado.productoN}", color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Remito último despacho: ${resultado.remitoN}", color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Fecha último despacho: ${resultado.fechaHora}", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                else -> Unit
            }
        }
    }
}
