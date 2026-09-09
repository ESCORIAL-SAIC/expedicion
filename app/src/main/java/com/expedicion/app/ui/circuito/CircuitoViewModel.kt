package com.expedicion.app.ui.circuito

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expedicion.app.data.ApiResult
import com.expedicion.app.data.circuito.Circuito
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

/**
 * Despacho de los circuitos IMPORT / PEABODY. Un solo ViewModel para los dos: la diferencia de
 * comportamiento vive del lado del servidor (que validaciones corren), no en la UI.
 *
 * El circuito llega por [inicializar] y no por SavedStateHandle a proposito: el resto del
 * proyecto construye los ViewModels por constructor directo, tanto en produccion (Hilt) como en
 * los tests, y un argumento de navegacion obligaria a introducir SavedStateHandle en todos los
 * sitios de construccion. La pantalla lo llama desde un LaunchedEffect y es idempotente.
 */
@HiltViewModel
class CircuitoViewModel @Inject constructor(
    private val remitoRepository: RemitoRepository,
    private val escaneoRepository: EscaneoRepository,
    private val soundPlayer: SoundFeedbackPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CircuitoUiState())
    val uiState: StateFlow<CircuitoUiState> = _uiState.asStateFlow()

    private var inicializado = false

    /** Fija el circuito de esta pantalla. Idempotente: solo el primer llamado tiene efecto. */
    fun inicializar(circuito: Circuito) {
        if (inicializado) return
        inicializado = true
        _uiState.update { it.copy(circuito = circuito) }
    }

    private val circuito: Circuito get() = _uiState.value.circuito

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
            when (val result = remitoRepository.listarCircuito(circuito, remitoN)) {
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

    /** Abre el listado completo sin autoseleccionar, igual que la lupa de Despacho. */
    fun abrirBuscador() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = remitoRepository.listarCircuito(circuito, "")) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, mostrarBuscador = true, remitosCandidatos = result.data.items)
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
                mostrarBuscador = false,
                etiquetaInput = "",
                ultimoProductoEscaneado = null,
                focusTrigger = it.focusTrigger + 1,
            )
        }
        cargarDetalle()
    }

    /**
     * El detalle se pide con esDespacho = true: los circuitos nuevos escriben en el staging de
     * despacho, asi que comparten la vista de transaccion.
     */
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
            val result = escaneoRepository.escanearCircuito(
                circuito = state.circuito,
                remitoId = remitoId,
                etiqueta = state.etiquetaInput,
                remitoN = state.remitoN,
            )
            when (result) {
                is ApiResult.Success -> when (val outcome = result.data) {
                    // En PEABODY el servidor no chequea duplicados (los EANs se repiten entre
                    // unidades), asi que esta rama practicamente no ocurre; se maneja igual que en
                    // Despacho por si el circuito se configura con la validacion activa.
                    is ScanOutcome.Duplicated -> _uiState.update {
                        it.copy(isLoading = false, etiquetaInput = "", focusTrigger = it.focusTrigger + 1)
                    }
                    is ScanOutcome.Success -> {
                        soundPlayer.playSuccess()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                etiquetaInput = "",
                                ultimoProductoEscaneado = outcome.productoN,
                                totalEscaneado = outcome.totalEscaneado,
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
                            // Falla de infraestructura: se preserva el contexto para reintentar.
                            it.copy(isLoading = false, errorMessage = result.message)
                        } else {
                            // Error de negocio: se limpia el campo y vuelve el foco.
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
        if (etiqueta.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = escaneoRepository.eliminarEtiquetaCircuito(circuito, remitoId, etiqueta)) {
                is ApiResult.Success -> {
                    soundPlayer.playSuccess()
                    _uiState.update {
                        it.copy(isLoading = false, totalEscaneado = result.data.totalEscaneado)
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
            when (val result = escaneoRepository.borrarTransaccionCircuito(circuito, remitoId)) {
                // Se preserva el circuito al resetear, o la pantalla quedaria apuntando a otro.
                is ApiResult.Success -> _uiState.update {
                    CircuitoUiState(circuito = it.circuito, cerrarPantalla = true)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    /** Valida cantidad == original en el servidor; si difiere, error y NO cierra. */
    fun confirmar() {
        val remitoId = _uiState.value.remitoId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = escaneoRepository.confirmarCircuito(circuito, remitoId)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, cerrarPantalla = true) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }
}
