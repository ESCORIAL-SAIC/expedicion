package com.expedicion.app.ui.login

data class LoginUiState(
    val usuario: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false,
    val mostrarConfirmarSalir: Boolean = false,
    /** Version de la app (BuildConfig.VERSION_NAME), fija. */
    val appVersion: String = "",
    /**
     * Version del API verificada al configurar la URL (persistida en ApiConfigStore). Null si
     * nunca se verifico una conexion.
     */
    val apiVersion: String? = null,
)
