package com.expedicion.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expedicion.app.BuildConfig
import com.expedicion.app.data.ApiResult
import com.expedicion.app.data.config.ApiConfigStore
import com.expedicion.app.data.isServerFault
import com.expedicion.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Texto client-side de "Datos de acceso incorrectos" (idem `Messages.INVALID_CREDENTIALS` del
 * API), usado unicamente cuando ambos campos estan vacios y no se llega a llamar al backend
 * (ver comentario de `submit()`).
 */
private const val INVALID_CREDENTIALS_MESSAGE = "Datos de acceso incorrectos. Vuelva a intentarlo."

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val apiConfigStore: ApiConfigStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState(appVersion = BuildConfig.VERSION_NAME))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // Se lee la version ya verificada y persistida, sin volver a consultar el API: el chequeo
        // real ocurre en ApiConfigScreen al guardar la URL (ver ApiProbe).
        viewModelScope.launch {
            val apiVersion = apiConfigStore.apiVersion.first()
            _uiState.update { it.copy(apiVersion = apiVersion) }
        }
    }

    fun onUsuarioChange(value: String) {
        _uiState.update { it.copy(usuario = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onLoginConsumed() {
        _uiState.update { it.copy(loginSuccess = false) }
    }

    fun solicitarSalir() {
        _uiState.update { it.copy(mostrarConfirmarSalir = true) }
    }

    fun cancelarSalir() {
        _uiState.update { it.copy(mostrarConfirmarSalir = false) }
    }

    /**
     * Regla client-side (unica de la pantalla, no viene del API): si ambos campos estan vacios,
     * no se llama al backend (replica UsuarioValido('','') en UnitIngreso.pas, que ni siquiera
     * consulta la base) pero el flujo Delphi igual limpia los campos y muestra el dialogo de
     * "datos incorrectos" (UnitIngreso.pas:59-76) — se replica esa parte tambien.
     */
    fun submit() {
        val usuario = _uiState.value.usuario
        val password = _uiState.value.password
        if (usuario.isBlank() && password.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = INVALID_CREDENTIALS_MESSAGE, usuario = "", password = "")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.login(usuario, password)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoading = false, loginSuccess = true, usuario = "", password = "")
                }
                is ApiResult.Error -> _uiState.update {
                    if (result.isServerFault()) {
                        // Falla de infraestructura (503/500): se preservan los campos para reintentar
                        // sin retipear, replicando el else de "sin conexion" de UnitIngreso.pas.
                        it.copy(isLoading = false, errorMessage = result.message)
                    } else {
                        // Error de negocio (credenciales invalidas): se limpian, igual al Delphi original.
                        it.copy(isLoading = false, errorMessage = result.message, usuario = "", password = "")
                    }
                }
            }
        }
    }
}
