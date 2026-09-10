package com.expedicion.app.ui.buscarremito

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.expedicion.app.data.remote.dto.RemitoListItemDto
import com.expedicion.app.ui.theme.ExpGreen
import com.expedicion.app.ui.theme.ExpGreenLight

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

    val grupos = remember(items, filtro) {
        val coincidentes = if (filtro.isBlank()) {
            items
        } else {
            items.filter {
                it.remitoN.contains(filtro, ignoreCase = true) || it.clienteN.contains(filtro, ignoreCase = true)
            }
        }
        // Un remito con productos de varios tipos llega como una fila por tipo, todas con el mismo
        // remitoId: se agrupan para que se vean como lo que son -- un remito con varios pedazos --
        // en vez de filas repetidas sueltas.
        //
        // El orden es por REMITO: los completos al final, pero el remito entero, sin partirse entre
        // pendientes y terminados. `sortedBy` es estable, asi que dentro de cada grupo se conserva
        // el orden por numero que ya trae el endpoint.
        //
        // El avance es del remito completo, asi que todas las filas de un grupo traen el mismo
        // valor: basta mirar la primera.
        coincidentes
            .groupBy { it.remitoId }
            .values
            .sortedBy { filas -> filas.first().completo }
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

            if (grupos.isEmpty()) {
                Text("No se encontraron remitos.", modifier = Modifier.padding(16.dp))
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // La key es el remitoId, que si es unico una vez agrupado.
                items(grupos, key = { filas -> filas.first().remitoId }) { filas ->
                    RemitoGrupo(filas = filas, onSeleccionar = onSeleccionar)
                }
            }
        }
    }
}

/**
 * Un remito y sus pedazos por tipo, como una sola tarjeta.
 *
 * El numero y el cliente van una vez en la cabecera, y abajo una fila clickeable por tipo. Con un
 * solo tipo la fila se muestra igual: mantiene la lectura uniforme y deja claro con que tipo se va
 * a escanear.
 */
@Composable
private fun RemitoGrupo(
    filas: List<RemitoListItemDto>,
    onSeleccionar: (RemitoListItemDto) -> Unit,
) {
    val cabecera = filas.first()
    val remitoCompleto = cabecera.completo

    Card(
        // Color opaco y no ExpGreen translucido: sobre el gris del bottom sheet el alpha se
        // ensuciaba y se leia gris verdoso en vez de verde.
        colors = CardDefaults.cardColors(
            containerColor = if (remitoCompleto) ExpGreenLight else MaterialTheme.colorScheme.surface,
        ),
        // El borde es lo que hace legible el agrupamiento: sin el, las filas de tipo de un remito
        // y las del siguiente se leen como una lista plana.
        border = BorderStroke(1.dp, if (remitoCompleto) ExpGreen else MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (remitoCompleto) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Remito completo",
                        tint = ExpGreen,
                        modifier = Modifier.padding(end = 8.dp).size(20.dp),
                    )
                }
                Column {
                    Text(cabecera.remitoN, style = MaterialTheme.typography.titleMedium)
                    Text(
                        cabecera.clienteN,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // El avance va en la cabecera porque es del remito completo: la API no puede
            // repartirlo por tipo (ver obtenerAvancePorRemito). Puesto en cada fila de tipo se
            // leeria como el avance de ese tipo, que no es.
            if (cabecera.cantidadPedida > 0) {
                Text(
                    "${cabecera.cantidadEscaneada}/${cabecera.cantidadPedida}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (remitoCompleto) ExpGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        filas.forEach { fila ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeleccionar(fila) }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            ) {
                Text(
                    fila.tipo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
