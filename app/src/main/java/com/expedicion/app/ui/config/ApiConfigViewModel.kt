package com.expedicion.app.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expedicion.app.BuildConfig
import com.expedicion.app.data.config.ApiConfigStore
import com.expedicion.app.data.config.ApiProbe
import com.expedicion.app.data.config.ApiProbeResult
import com.expedicion.app.data.config.ApiUrlValidationResult
import com.expedicion.app.data.config.ApiUrlValidator
import com.expedicion.app.ui.common.apiVersionLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiConfigViewModel @Inject constructor(
    private val apiConfigStore: ApiConfigStore,
    private val apiProbe: ApiProbe,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiConfigUiState(appVersion = BuildConfig.VERSION_NAME))
    val uiState: StateFlow<ApiConfigUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val urlGuardada = apiConfigStore.apiBaseUrl.first()
            val versionGuardada = apiConfigStore.apiVersion.first()
            _uiState.update {
                it.copy(
                    // Si nunca se guardó nada, se precarga el default de build-time como sugerencia,
                    // pero eso no cuenta como "ya configurado" hasta que el usuario guarde explícitamente.
                    urlInput = urlGuardada ?: BuildConfig.API_BASE_URL,
                    isInitialSetup = urlGuardada == null,
                    apiVersion = versionGuardada,
                    isLoading = false,
                )
            }
        }
    }

    fun onUrlChange(value: String) {
        // Cualquier edición invalida la verificación anterior: la URL en pantalla ya no es la que
        // se sondeó, así que no puede quedar un "conexión OK" colgado de otra dirección.
        _uiState.update { it.copy(urlInput = value, errorMessage = null, verificacionOk = null) }
    }

    /**
     * Valida formato, sondea el API y sólo persiste si responde OK. Un formato inválido, un
     * servidor inalcanzable o un `/health/ready` que no da 200 bloquean el guardado: la app no
     * debe quedar configurada contra una dirección que no sirve.
     */
    fun guardar() {
        if (_uiState.value.isVerificando) return

        val state = _uiState.value
        when (val resultado = ApiUrlValidator.validate(state.urlInput)) {
            is ApiUrlValidationResult.Invalid ->
                _uiState.update { it.copy(errorMessage = resultado.message, verificacionOk = null) }

            is ApiUrlValidationResult.Valid -> viewModelScope.launch {
                _uiState.update {
                    it.copy(isVerificando = true, errorMessage = null, verificacionOk = null)
                }

                when (val probe = apiProbe.probe(resultado.normalizedUrl)) {
                    is ApiProbeResult.Ok -> {
                        apiConfigStore.setApiBaseUrl(resultado.normalizedUrl, probe.version)
                        _uiState.update {
                            it.copy(
                                isVerificando = false,
                                apiVersion = probe.version,
                                verificacionOk = "Conexión establecida con ${apiVersionLabel(probe.version)}.",
                                guardadoExitoso = true,
                            )
                        }
                    }

                    is ApiProbeResult.Unreachable -> falloVerificacion(
                        "No se pudo conectar con el servidor en esa dirección. " +
                            "Verifique la URL, que el servidor esté encendido y su conexión de red.",
                    )

                    is ApiProbeResult.DbUnavailable -> falloVerificacion(
                        "El servidor responde pero no puede operar: " +
                            describirBases(probe.postgresOk, probe.mssqlOk) +
                            ". Revise las bases de datos antes de continuar.",
                    )

                    is ApiProbeResult.NotHealthy -> falloVerificacion(
                        "Esa dirección responde pero no es un servidor de Expedición válido " +
                            "(${probe.detalle}).",
                    )
                }
            }
        }
    }

    private fun falloVerificacion(mensaje: String) {
        _uiState.update {
            it.copy(isVerificando = false, errorMessage = mensaje, verificacionOk = null)
        }
    }

    private fun describirBases(postgresOk: Boolean, mssqlOk: Boolean): String = when {
        !postgresOk && !mssqlOk -> "no responden las bases Postgres (ESCORIAL) ni SQL Server (Etiquetas)"
        !postgresOk -> "no responde la base Postgres (ESCORIAL)"
        else -> "no responde la base SQL Server (Etiquetas)"
    }
}
