package com.expedicion.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponseDto(
    val valid: Boolean = false,
)

@Serializable
data class RemitoListItemDto(
    val remitoN: String,
    val clienteN: String,
    val remitoId: String,
    val clienteId: String,
    val tipo: String,
    val consignacion: Boolean? = null,
)

@Serializable
data class RemitoListResponseDto(
    val exactMatch: RemitoListItemDto? = null,
    val items: List<RemitoListItemDto> = emptyList(),
)

@Serializable
data class VistaTransaccionItemDto(
    val itemRemitoId: String? = null,
    val productoId: String? = null,
    val productoN: String,
    val cantidad: Int,
    val cantidadOriginal: Int,
    val cantidadRestante: Int,
)

@Serializable
data class ProductoValidoDto(
    val itemRemitoId: String,
    val productoId: String,
)

@Serializable
data class DetalleRemitoResponseDto(
    val items: List<VistaTransaccionItemDto> = emptyList(),
    val totalEscaneado: Int = 0,
    val productosValidos: List<ProductoValidoDto>? = null,
)

/**
 * Respuesta polimorfica de POST .../escaneo: exito (`success:true`, 201) o duplicado silencioso
 * (`duplicated:true`, 200). Se modela con todos los campos opcionales y se discrimina por
 * presencia de `duplicated` (ver EscaneoRepository.mapScanResponse).
 */
@Serializable
data class ScanResponseDto(
    val success: Boolean? = null,
    val duplicated: Boolean? = null,
    val productoN: String? = null,
    val cantidadEscaneada: Int? = null,
    val cantidadRestante: Int? = null,
    val totalEscaneado: Int? = null,
)

@Serializable
data class EliminarResponseDto(
    val success: Boolean,
    val totalEscaneado: Int,
)

@Serializable
data class SuccessResponseDto(
    val success: Boolean = true,
)

/**
 * Respuesta polimorfica de POST /etiquetas/estado: abort silencioso (`aborted:true`), disponible
 * (`available:true`) o no disponible (`available:false` + datos del ultimo despacho).
 */
@Serializable
data class EstadoResponseDto(
    val aborted: Boolean? = null,
    val available: Boolean? = null,
    val productoN: String? = null,
    val clienteN: String? = null,
    val remitoN: String? = null,
    val fechaHora: String? = null,
)

@Serializable
data class ErrorBodyDto(
    val code: String,
    val message: String,
)

@Serializable
data class ErrorEnvelopeDto(
    val error: ErrorBodyDto,
)
