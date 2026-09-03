package com.expedicion.app.ui.devolucion

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

@HiltViewModel
class DevolucionViewModel @Inject constructor(
    private val remitoRepository: RemitoRepository,
    private val escaneoRepository: EscaneoRepository,
    private val soundPlayer: SoundFeedbackPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevolucionUiState())
    val uiState: StateFlow<DevolucionUiState> = _uiState.asStateFlow()

    fun onEtiquetaChange(value: String) {
        _uiState.update { it.copy(etiquetaInput = value) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onCerrarPantallaConsumido() {
        _uiState.update { it.copy(cerrarPantalla = false) }
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
            when (val result = remitoRepository.listarDevolucion(remitoN)) {
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
            when (val result = remitoRepository.detalle(remitoId, esDespacho = false)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, items = result.data.items) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    fun escanear() {
        val state = _uiState.value
        val remitoId = state.remitoId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = escaneoRepository.escanear(false, remitoId, state.etiquetaInput, state.tipo, state.remitoN)) {
                is ApiResult.Success -> when (val outcome = result.data) {
                    is ScanOutcome.Duplicated -> {
                        _uiState.update {
                            it.copy(isLoading = false, etiquetaInput = "", focusTrigger = it.focusTrigger + 1)
                        }
                    }
                    is ScanOutcome.Success -> {
                        soundPlayer.playSuccess()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                etiquetaInput = "",
                                ultimoProductoEscaneado = outcome.productoN,
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
                            it.copy(isLoading = false, errorMessage = result.message)
                        } else {
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
            when (val result = escaneoRepository.eliminarEtiqueta(false, remitoId, etiqueta, tipo)) {
                is ApiResult.Success -> {
                    soundPlayer.playSuccess()
                    _uiState.update { it.copy(isLoading = false) }
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
            when (val result = escaneoRepository.borrarTransaccion(false, remitoId)) {
                is ApiResult.Success -> _uiState.update { DevolucionUiState(cerrarPantalla = true) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    /**
     * Replica UnitDevolucion.pas ButtonConfirmarClick: el bloque de validacion esta comentado en
     * el original, siempre cierra sin chequear nada (ni siquiera el resultado de la llamada).
     */
    fun confirmar() {
        val remitoId = _uiState.value.remitoId
        if (remitoId == null) {
            _uiState.update { it.copy(cerrarPantalla = true) }
            return
        }
        viewModelScope.launch {
            escaneoRepository.confirmarDevolucion(remitoId)
            _uiState.update { it.copy(cerrarPantalla = true) }
        }
    }
}
