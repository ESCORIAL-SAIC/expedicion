package com.expedicion.app.ui.estado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expedicion.app.data.ApiResult
import com.expedicion.app.data.repository.EstadoOutcome
import com.expedicion.app.data.repository.EstadoRepository
import com.expedicion.app.sound.SoundFeedbackPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EstadoViewModel @Inject constructor(
    private val estadoRepository: EstadoRepository,
    private val soundPlayer: SoundFeedbackPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EstadoUiState())
    val uiState: StateFlow<EstadoUiState> = _uiState.asStateFlow()

    fun onTipoChange(value: String) {
        _uiState.update { it.copy(tipo = value, resultado = null) }
    }

    fun onEtiquetaChange(value: String) {
        _uiState.update { it.copy(etiquetaInput = value) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consultar() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, resultado = null) }
            when (val result = estadoRepository.consultar(state.tipo, state.etiquetaInput)) {
                is ApiResult.Success -> when (val outcome = result.data) {
                    is EstadoOutcome.Aborted -> _uiState.update { it.copy(isLoading = false) }
                    else -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            resultado = outcome,
                            etiquetaInput = "",
                            focusTrigger = it.focusTrigger + 1,
                        )
                    }
                }
                is ApiResult.Error -> {
                    soundPlayer.playError()
                    _uiState.update {
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
