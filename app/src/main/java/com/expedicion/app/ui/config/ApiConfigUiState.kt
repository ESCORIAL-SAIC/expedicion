package com.expedicion.app.ui.config

data class ApiConfigUiState(
    val urlInput: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = true,
    val isInitialSetup: Boolean = true,
    val guardadoExitoso: Boolean = false,
    /** True mientras corre el sondeo de conectividad contra la URL tipeada. */
    val isVerificando: Boolean = false,
    /**
     * Mensaje de exito de la verificacion, con la version del API. Presente sólo cuando el
     * sondeo dio OK; se limpia al editar la URL.
     */
    val verificacionOk: String? = null,
    /** Version del API guardada/verificada, para mostrarla en la pantalla. Null si nunca se verificó. */
    val apiVersion: String? = null,
    /** Version de la app (BuildConfig.VERSION_NAME), fija. */
    val appVersion: String = "",
)
