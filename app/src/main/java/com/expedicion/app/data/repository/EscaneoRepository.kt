package com.expedicion.app.data.repository

import com.expedicion.app.data.ApiResult
import com.expedicion.app.data.map
import com.expedicion.app.data.remote.ApiService
import com.expedicion.app.data.remote.dto.CredencialesRequest
import com.expedicion.app.data.remote.dto.EliminarEtiquetaRequest
import com.expedicion.app.data.remote.dto.EliminarResponseDto
import com.expedicion.app.data.remote.dto.EscaneoRequest
import com.expedicion.app.data.remote.dto.ScanResponseDto
import com.expedicion.app.data.remote.safeApiCall
import com.expedicion.app.session.SessionManager
import javax.inject.Inject

sealed interface ScanOutcome {
    data class Success(
        val productoN: String,
        val cantidadEscaneada: Int,
        val cantidadRestante: Int,
        val totalEscaneado: Int,
    ) : ScanOutcome

    data object Duplicated : ScanOutcome
}

private fun ScanResponseDto.toOutcome(): ScanOutcome =
    if (duplicated == true) {
        ScanOutcome.Duplicated
    } else {
        ScanOutcome.Success(
            productoN = productoN.orEmpty(),
            cantidadEscaneada = cantidadEscaneada ?: 0,
            cantidadRestante = cantidadRestante ?: 0,
            totalEscaneado = totalEscaneado ?: 0,
        )
    }

class EscaneoRepository @Inject constructor(
    private val api: ApiService,
    private val sessionManager: SessionManager,
) {
    private fun credenciales(): CredencialesRequest {
        val creds = sessionManager.current()
        return CredencialesRequest(creds?.usuario.orEmpty(), creds?.password.orEmpty())
    }

    suspend fun escanear(
        esDespacho: Boolean,
        remitoId: String,
        etiqueta: String,
        tipo: String,
        remitoN: String,
    ): ApiResult<ScanOutcome> {
        val creds = sessionManager.current()
        val body = EscaneoRequest(
            usuario = creds?.usuario.orEmpty(),
            password = creds?.password.orEmpty(),
            etiqueta = etiqueta,
            tipo = tipo,
            remitoN = remitoN,
        )
        return safeApiCall {
            if (esDespacho) api.escanearDespacho(remitoId, body) else api.escanearDevolucion(remitoId, body)
        }.map { it.toOutcome() }
    }

    suspend fun eliminarEtiqueta(
        esDespacho: Boolean,
        remitoId: String,
        etiqueta: String,
        tipo: String,
    ): ApiResult<EliminarResponseDto> {
        val creds = sessionManager.current()
        val body = EliminarEtiquetaRequest(
            usuario = creds?.usuario.orEmpty(),
            password = creds?.password.orEmpty(),
            etiqueta = etiqueta,
            tipo = tipo,
        )
        return safeApiCall {
            if (esDespacho) api.eliminarEtiquetaDespacho(remitoId, body) else api.eliminarEtiquetaDevolucion(remitoId, body)
        }
    }

    suspend fun borrarTransaccion(esDespacho: Boolean, remitoId: String): ApiResult<Unit> {
        val body = credenciales()
        return safeApiCall {
            if (esDespacho) api.borrarTransaccionDespacho(remitoId, body) else api.borrarTransaccionDevolucion(remitoId, body)
        }.map { }
    }

    suspend fun confirmarDespacho(remitoId: String): ApiResult<Unit> =
        safeApiCall { api.confirmarDespacho(remitoId, credenciales()) }.map { }

    suspend fun confirmarDevolucion(remitoId: String): ApiResult<Unit> =
        safeApiCall { api.confirmarDevolucion(remitoId, credenciales()) }.map { }
}
