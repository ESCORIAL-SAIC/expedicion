package com.expedicion.app.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expedicion.app.ui.common.ExpScaffold
import com.expedicion.app.ui.theme.ExpBlueDark
import com.expedicion.app.ui.theme.ExpBlueLight

private val TILE_HEIGHT = 110.dp

@Composable
fun MenuScreen(
    onDespacho: () -> Unit,
    onDevolucion: () -> Unit,
    onEstado: () -> Unit,
    onImportado: () -> Unit = {},
    onPeabody: () -> Unit = {},
    onAbrirConfiguracion: () -> Unit = {},
) {
    ExpScaffold(
        titulo = "Expedición",
        actions = {
            IconButton(onClick = onAbrirConfiguracion) {
                Icon(Icons.Filled.Settings, contentDescription = "Configuración del servidor")
            }
        },
    ) { padding ->
        // Con cinco entradas el reparto por weight() dejaba tiles de ~100dp, apretados para un
        // handheld con guantes: se pasa a alto fijo y scroll vertical.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MenuTile(
                icono = Icons.Filled.LocalShipping,
                texto = "Despacho",
                onClick = onDespacho,
                modifier = Modifier.fillMaxWidth().height(TILE_HEIGHT),
            )
            MenuTile(
                icono = Icons.Filled.AssignmentReturn,
                texto = "Devolución",
                onClick = onDevolucion,
                modifier = Modifier.fillMaxWidth().height(TILE_HEIGHT),
            )
            MenuTile(
                icono = Icons.Filled.Flight,
                texto = "Importados",
                onClick = onImportado,
                modifier = Modifier.fillMaxWidth().height(TILE_HEIGHT),
            )
            MenuTile(
                icono = Icons.Filled.Kitchen,
                texto = "Peabody",
                onClick = onPeabody,
                modifier = Modifier.fillMaxWidth().height(TILE_HEIGHT),
            )
            MenuTile(
                icono = Icons.Filled.Inventory,
                texto = "Estado Etiqueta",
                onClick = onEstado,
                modifier = Modifier.fillMaxWidth().height(TILE_HEIGHT),
            )
        }
    }
}

@Composable
private fun MenuTile(
    icono: ImageVector,
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ExpBlueLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = ExpBlueDark,
                modifier = Modifier.size(40.dp),
            )
            Text(
                texto,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = ExpBlueDark,
            )
        }
    }
}
