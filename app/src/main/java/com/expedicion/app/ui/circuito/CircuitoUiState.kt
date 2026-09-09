package com.expedicion.app.ui.circuito

import com.expedicion.app.data.circuito.Circuito
import com.expedicion.app.data.remote.dto.RemitoListItemDto
import com.expedicion.app.data.remote.dto.VistaTransaccionItemDto

/**
 * Estado de las pantallas de circuito (Importados / Peabody).
 *
 * Es el mismo estado que DespachoUiState menos el contador: el contador de a 8 cuenta bultos de
 * la linea propia y no aplica a estos productos.
 */
data class CircuitoUiState(
    val circuito: Circuito = Circuito.IMPORTADO,
    val remitoN: String = "",
    val clienteN: String = "",
    val remitoId: String? = null,
    val etiquetaInput: String = "",
    val items: List<VistaTransaccionItemDto> = emptyList(),
    val totalEscaneado: Int = 0,
    val ultimoProductoEscaneado: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val remitosCandidatos: List<RemitoListItemDto> = emptyList(),
    val mostrarBuscador: Boolean = false,
    val mostrarConfirmarBorrarTransaccion: Boolean = false,
    val cerrarPantalla: Boolean = false,
    val focusTrigger: Int = 0,
) {
    val tieneRemitoSeleccionado: Boolean get() = remitoId != null
}
