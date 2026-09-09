package com.expedicion.app.data.repository

import com.expedicion.app.data.ApiResult
import com.expedicion.app.data.circuito.Circuito
import com.expedicion.app.data.remote.ApiService
import com.expedicion.app.data.remote.dto.DetalleRemitoResponseDto
import com.expedicion.app.data.remote.dto.RemitoListResponseDto
import com.expedicion.app.data.remote.safeApiCall
import javax.inject.Inject

class RemitoRepository @Inject constructor(
    private val api: ApiService,
) {
    suspend fun listarDespacho(remitoN: String): ApiResult<RemitoListResponseDto> =
        safeApiCall { api.listarRemitosDespacho(remitoN) }

    suspend fun listarDevolucion(remitoN: String): ApiResult<RemitoListResponseDto> =
        safeApiCall { api.listarRemitosDevolucion(remitoN) }

    suspend fun detalle(remitoId: String, esDespacho: Boolean): ApiResult<DetalleRemitoResponseDto> =
        safeApiCall { api.detalleRemito(remitoId, esDespacho) }

    /**
     * Listado de remitos de un circuito nuevo (IMPORT / PEABODY). El detalle se sigue pidiendo con
     * `detalle(remitoId, esDespacho = true)`: estos circuitos escriben en el staging de despacho,
     * asi que la vista de transaccion es la misma.
     */
    suspend fun listarCircuito(circuito: Circuito, remitoN: String): ApiResult<RemitoListResponseDto> =
        safeApiCall { api.listarRemitosCircuito(circuito.slug, remitoN) }
}
