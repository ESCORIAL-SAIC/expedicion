package com.expedicion.app.ui.circuito

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expedicion.app.data.circuito.Circuito
import com.expedicion.app.ui.buscarremito.BuscarRemitoBottomSheet
import com.expedicion.app.ui.common.ConfirmDialog
import com.expedicion.app.ui.common.ErrorDialog
import com.expedicion.app.ui.common.ExpScaffold
import com.expedicion.app.ui.theme.ExpGreen
import com.expedicion.app.ui.theme.ExpRed

/**
 * Pantalla de despacho de los circuitos IMPORT / PEABODY. Es la de Despacho sin el contador de
 * bultos (no aplica a estos productos) y sin el campo Tipo en la tarjeta: el circuito ya esta
 * fijado por la pantalla y se muestra en el titulo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitoScreen(
    circuito: Circuito,
    onVolver: () -> Unit,
    viewModel: CircuitoViewModel = hiltViewModel(),
) {
    LaunchedEffect(circuito) {
        viewModel.inicializar(circuito)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val etiquetaFocusRequester = remember { FocusRequester() }
    var remitoInput by remember { mutableStateOf("") }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.focusTrigger) {
        etiquetaFocusRequester.requestFocus()
    }

    LaunchedEffect(uiState.cerrarPantalla) {
        if (uiState.cerrarPantalla) {
            viewModel.onCerrarPantallaConsumido()
            onVolver()
        }
    }

    if (uiState.errorMessage != null) {
        ErrorDialog(message = uiState.errorMessage!!, onDismiss = viewModel::dismissError)
    }

    if (uiState.mostrarBuscador) {
        BuscarRemitoBottomSheet(
            items = uiState.remitosCandidatos,
            onSeleccionar = { viewModel.seleccionarRemito(it) },
            onDismiss = viewModel::dismissBuscador,
        )
    }

    if (uiState.mostrarConfirmarBorrarTransaccion) {
        ConfirmDialog(
            message = "¿Está seguro que desea eliminar la transacción?",
            onConfirm = viewModel::confirmarBorrarTransaccion,
            onDismiss = viewModel::cancelarBorrarTransaccion,
        )
    }

    if (mostrarDialogoEliminar) {
        EliminarEtiquetaCircuitoDialog(
            onConfirmar = { etiqueta -> viewModel.eliminarEtiqueta(etiqueta); mostrarDialogoEliminar = false },
            onDismiss = { mostrarDialogoEliminar = false },
        )
    }

    ExpScaffold(titulo = circuito.titulo, onBack = onVolver) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = remitoInput,
                onValueChange = { remitoInput = it },
                label = { Text("Remito") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.buscarRemito(remitoInput) }),
                trailingIcon = {
                    IconButton(onClick = viewModel::abrirBuscador) {
                        Icon(Icons.Filled.Search, contentDescription = "Ver listado de remitos")
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )

            OutlinedTextField(
                value = uiState.etiquetaInput,
                onValueChange = viewModel::onEtiquetaChange,
                label = { Text("Etiqueta") },
                singleLine = true,
                enabled = uiState.tieneRemitoSeleccionado,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.escanear() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(etiquetaFocusRequester)
                    .focusable()
                    .padding(bottom = 8.dp),
            )

            uiState.ultimoProductoEscaneado?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Cliente", style = MaterialTheme.typography.labelMedium)
                    Text(uiState.clienteN, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Total", style = MaterialTheme.typography.labelMedium)
                    Text(uiState.totalEscaneado.toString(), style = MaterialTheme.typography.titleMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = viewModel::escanear,
                    enabled = uiState.tieneRemitoSeleccionado,
                    colors = ButtonDefaults.buttonColors(containerColor = ExpGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("Nuevo") }
                OutlinedButton(
                    onClick = { mostrarDialogoEliminar = true },
                    enabled = uiState.tieneRemitoSeleccionado,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpRed),
                    border = BorderStroke(1.dp, ExpRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("Eliminar") }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = viewModel::confirmar,
                    enabled = uiState.tieneRemitoSeleccionado,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("Confirmar") }
                OutlinedButton(
                    onClick = viewModel::solicitarBorrarTransaccion,
                    enabled = uiState.tieneRemitoSeleccionado,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpRed),
                    border = BorderStroke(1.dp, ExpRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("Borrar transacción") }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.items, key = { it.itemRemitoId ?: it.productoN }) { item ->
                    ListItem(
                        headlineContent = { Text(item.productoN) },
                        trailingContent = { Text("${item.cantidad}/${item.cantidadOriginal}") },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EliminarEtiquetaCircuitoDialog(onConfirmar: (String) -> Unit, onDismiss: () -> Unit) {
    var etiqueta by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar etiqueta") },
        text = {
            OutlinedTextField(
                value = etiqueta,
                onValueChange = { etiqueta = it },
                label = { Text("Identificador etiqueta") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(etiqueta) }) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
