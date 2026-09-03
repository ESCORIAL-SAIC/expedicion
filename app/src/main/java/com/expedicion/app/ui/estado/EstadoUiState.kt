package com.expedicion.app.ui.estado

import com.expedicion.app.data.repository.EstadoOutcome

/** Catalogo cerrado de TIPO observado en el sistema legado (bloqueante #3 del plan). */
val TIPOS_DISPONIBLES = listOf("COCINA", "TERMOTANQUE")

data class EstadoUiState(
    val tipo: String = "",
    val etiquetaInput: String = "",
    val resultado: EstadoOutcome? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val focusTrigger: Int = 0,
)
