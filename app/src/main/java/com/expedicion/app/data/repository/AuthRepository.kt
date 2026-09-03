package com.expedicion.app.data.repository

import com.expedicion.app.data.ApiResult
import com.expedicion.app.data.map
import com.expedicion.app.data.remote.ApiService
import com.expedicion.app.data.remote.dto.CredencialesRequest
import com.expedicion.app.data.remote.safeApiCall
import com.expedicion.app.session.SessionManager
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val sessionManager: SessionManager,
) {
    /**
     * La regla "ambos campos vacios no llama al API" es responsabilidad del LoginViewModel
     * (client-side puro, no hay mensaje de backend para ese caso).
     */
    suspend fun login(usuario: String, password: String): ApiResult<Unit> {
        val result = safeApiCall { api.login(CredencialesRequest(usuario, password)) }
        if (result is ApiResult.Success) {
            sessionManager.setCredentials(usuario, password)
        }
        return result.map { }
    }

    fun logout() {
        sessionManager.clear()
    }
}
