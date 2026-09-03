package com.expedicion.app.ui.despacho

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expedicion.app.data.ApiResult
import com.expedicion.app.data.isServerFault
import com.expedicion.app.data.remote.dto.RemitoListItemDto
import com.expedicion.app.data.repository.EscaneoRepository
import com.expedicion.app.data.repository.RemitoRepository
import com.expedicion.app.data.repository.ScanOutcome
import com.expedicion.app.sound.SoundFeedbackPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CONTADOR_MAX = 8

@HiltViewModel
class DespachoViewModel @Inject constructor(
    private val remitoRepository: RemitoRepository,
    private val escaneoRepository: EscaneoRepository,
    private val soundPlayer: SoundFeedbackPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DespachoUiState())
    val uiState: StateFlow<DespachoUiState> = _uiState.asStateFlow()

    fun onEtiquetaChange(value: String) {
        _uiState.update { it.copy(etiquetaInput = value) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onCerrarPantallaConsumido() {
        _uiState.update { it.copy(cerrarPantalla = false) }
    }

    fun resetContador() {
        _uiState.update { it.copy(contador = 0) }
    }

    fun dismissBuscador() {
        _uiState.update { it.copy(mostrarBuscador = false) }
    }

    fun solicitarBorrarTransaccion() {
        if (_uiState.value.remitoId == null) return
        _uiState.update { it.copy(mostrarConfirmarBorrarTransaccion = true) }
    }

    fun cancelarBorrarTransaccion() {
        _uiState.update { it.copy(mostrarConfirmarBorrarTransaccion = false) }
    }

    fun buscarRemito(remitoN: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = remitoRepository.listarDespacho(remitoN)) {
                is ApiResult.Success -> {
                    val body = result.data
                    if (body.exactMatch != null) {
                        seleccionarRemito(body.exactMatch)
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, mostrarBuscador = true, remitosCandidatos = body.items)
                        }
                    }
                }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    fun seleccionarRemito(remito: RemitoListItemDto) {
        _uiState.update {
            it.copy(
                remitoN = remito.remitoN,
                clienteN = remito.clienteN,
                remitoId = remito.remitoId,
                tipo = remito.tipo,
                mostrarBuscador = false,
                etiquetaInput = "",
                ultimoProductoEscaneado = null,
                focusTrigger = it.focusTrigger + 1,
            )
        }
        cargarDetalle()
    }

    private fun cargarDetalle() {
        val remitoId = _uiState.value.remitoId ?: return
        viewModelScope.launch {
            when (val result = remitoRepository.detalle(remitoId, esDespacho = true)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, items = result.data.items, totalEscaneado = result.data.totalEscaneado)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    fun escanear() {
        val state = _uiState.value
        val remitoId = state.remitoId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = escaneoRepository.escanear(true, remitoId, state.etiquetaInput, state.tipo, state.remitoN)) {
                is ApiResult.Success -> when (val outcome = result.data) {
                    is ScanOutcome.Duplicated -> {
                        // Abort silencioso: sin dialogo, sin sonido, solo limpia el campo y devuelve foco.
                        _uiState.update {
                            it.copy(isLoading = false, etiquetaInput = "", focusTrigger = it.focusTrigger + 1)
                        }
                    }
                    is ScanOutcome.Success -> {
                        soundPlayer.playSuccess()
                        _uiState.update {
                            val nuevoContador = if (it.contador >= CONTADOR_MAX) 1 else it.contador + 1
                            it.copy(
                                isLoading = false,
                                etiquetaInput = "",
                                ultimoProductoEscaneado = outcome.productoN,
                                totalEscaneado = outcome.totalEscaneado,
                                contador = nuevoContador,
                                focusTrigger = it.focusTrigger + 1,
                            )
                        }
                        cargarDetalle()
                    }
                }
                is ApiResult.Error -> {
                    soundPlayer.playError()
                    _uiState.update {
                        if (result.isServerFault()) {
                            // Falla de infraestructura (503/500): no se resetea ni se limpia el campo,
                            // el usuario puede reintentar sin perder contexto (remito, items, contador).
                            it.copy(isLoading = false, errorMessage = result.message)
                        } else {
                            // Error de negocio (400/404/409/422): se limpia el campo y vuelve el foco,
                            // igual que el Delphi original.
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message,
                                etiquetaInput = "",
                                focusTrigger = it.focusTrigger + 1,
                            )
                        }
                    }
                }
            }
        }
    }

    fun eliminarEtiqueta(etiqueta: String) {
        val remitoId = _uiState.value.remitoId ?: return
        val tipo = _uiState.value.tipo
        if (etiqueta.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = escaneoRepository.eliminarEtiqueta(true, remitoId, etiqueta, tipo)) {
                is ApiResult.Success -> {
                    soundPlayer.playSuccess()
                    _uiState.update {
                        val nuevoContador = if (it.contador <= 0) 0 else it.contador - 1
                        it.copy(isLoading = false, totalEscaneado = result.data.totalEscaneado, contador = nuevoContador)
                    }
                    cargarDetalle()
                }
                is ApiResult.Error -> {
                    soundPlayer.playError()
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun confirmarBorrarTransaccion() {
        val remitoId = _uiState.value.remitoId ?: return
        _uiState.update { it.copy(mostrarConfirmarBorrarTransaccion = false) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = escaneoRepository.borrarTransaccion(true, remitoId)) {
                is ApiResult.Success -> _uiState.update { DespachoUiState(cerrarPantalla = true) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    /**
     * Valida cantidad == original en el servidor; si difiere, error y NO cierra. Sin diferencias,
     * confirma y cierra.
     */
    fun confirmar() {
        val remitoId = _uiState.value.remitoId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = escaneoRepository.confirmarDespacho(remitoId)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, cerrarPantalla = true) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }
}
