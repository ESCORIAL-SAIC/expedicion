package com.expedicion.app.ui.buscarremito

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.expedicion.app.data.remote.dto.RemitoListItemDto

/**
 * Listado buscable de remitos. Se abre por dos caminos: cuando la busqueda por numero exacto no
 * matcheo (RemitoRepository.exactMatch == null), o cuando el usuario toca la lupa del campo Remito
 * para ver el listado completo sin tipear nada. El filtro de texto es 100% client-side sobre la
 * lista ya traida por el endpoint (no vuelve a golpear el API por cada tecla).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuscarRemitoBottomSheet(
    items: List<RemitoListItemDto>,
    onSeleccionar: (RemitoListItemDto) -> Unit,
    onDismiss: () -> Unit,
) {
    var filtro by remember { mutableStateOf("") }
    val filtrados = remember(items, filtro) {
        if (filtro.isBlank()) {
            items
        } else {
            items.filter {
                it.remitoN.contains(filtro, ignoreCase = true) || it.clienteN.contains(filtro, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = filtro,
                onValueChange = { filtro = it },
                label = { Text("Filtrar por remito o cliente") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )

            if (filtrados.isEmpty()) {
                Text("No se encontraron remitos.", modifier = Modifier.padding(16.dp))
            }

            LazyColumn {
                // La clave lleva el indice porque remitoId NO es unico en esta lista: la query de la
                // API agrupa por TIPO, asi que un remito con items de varios tipos devuelve una fila
                // por tipo con el mismo remitoId. Con key = { it.remitoId } Compose crashea
                // ("Key ... was already used") al listar todo desde la lupa.
                itemsIndexed(filtrados, key = { index, item -> "${item.remitoId}|${item.tipo}|$index" }) { _, remito ->
                    ListItem(
                        headlineContent = { Text(remito.remitoN) },
                        supportingContent = { Text(remito.clienteN) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeleccionar(remito) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
