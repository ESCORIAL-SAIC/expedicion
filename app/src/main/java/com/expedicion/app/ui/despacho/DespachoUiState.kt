package com.expedicion.app.ui.despacho

import com.expedicion.app.data.remote.dto.RemitoListItemDto
import com.expedicion.app.data.remote.dto.VistaTransaccionItemDto

data class DespachoUiState(
    val remitoN: String = "",
    val clienteN: String = "",
    val remitoId: String? = null,
    val tipo: String = "",
    val etiquetaInput: String = "",
    val items: List<VistaTransaccionItemDto> = emptyList(),
    val totalEscaneado: Int = 0,
    val contador: Int = 0,
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
