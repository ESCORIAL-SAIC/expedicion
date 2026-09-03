package com.expedicion.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expedicion.app.data.config.ApiConfigStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decide, antes de armar el `NavHost`, si hay que arrancar en `ApiConfig` (nunca se guardó una
 * URL) o directo en `Login`. `null` mientras todavía no se leyó el DataStore.
 */
@HiltViewModel
class AppStartViewModel @Inject constructor(
    private val apiConfigStore: ApiConfigStore,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val urlGuardada = apiConfigStore.apiBaseUrl.first()
            _startDestination.value = if (urlGuardada == null) Routes.API_CONFIG else Routes.LOGIN
        }
    }
}
