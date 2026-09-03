package com.expedicion.app.data.repository

import com.expedicion.app.data.ApiResult
import com.expedicion.app.data.map
import com.expedicion.app.data.remote.ApiService
import com.expedicion.app.data.remote.dto.EstadoRequest
import com.expedicion.app.data.remote.dto.EstadoResponseDto
import com.expedicion.app.data.remote.safeApiCall
import com.expedicion.app.session.SessionManager
import javax.inject.Inject

sealed interface EstadoOutcome {
    data object Aborted : EstadoOutcome
    data class Disponible(val productoN: String) : EstadoOutcome
    data class NoDisponible(
        val clienteN: String,
        val productoN: String,
        val remitoN: String,
        val fechaHora: String,
    ) : EstadoOutcome
}

private fun EstadoResponseDto.toOutcome(): EstadoOutcome = when {
    aborted == true -> EstadoOutcome.Aborted
    available == true -> EstadoOutcome.Disponible(productoN.orEmpty())
    else -> EstadoOutcome.NoDisponible(
        clienteN = clienteN.orEmpty(),
        productoN = productoN.orEmpty(),
        remitoN = remitoN.orEmpty(),
        fechaHora = fechaHora.orEmpty(),
    )
}

class EstadoRepository @Inject constructor(
    private val api: ApiService,
    private val sessionManager: SessionManager,
) {
    suspend fun consultar(tipo: String, etiqueta: String): ApiResult<EstadoOutcome> {
        val creds = sessionManager.current()
        val body = EstadoRequest(
            usuario = creds?.usuario.orEmpty(),
            password = creds?.password.orEmpty(),
            tipo = tipo,
            etiqueta = etiqueta,
        )
        return safeApiCall { api.consultarEstado(body) }.map { it.toOutcome() }
    }
}
