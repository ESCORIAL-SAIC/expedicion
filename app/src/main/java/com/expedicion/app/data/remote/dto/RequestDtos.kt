package com.expedicion.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Body comun a login, confirmar y borrar transaccion: solo credenciales. */
@Serializable
data class CredencialesRequest(
    val usuario: String,
    val password: String,
)

@Serializable
data class EscaneoRequest(
    val usuario: String,
    val password: String,
    val etiqueta: String,
    val tipo: String,
    val remitoN: String,
)

@Serializable
data class EliminarEtiquetaRequest(
    val usuario: String,
    val password: String,
    val etiqueta: String,
    val tipo: String,
)

@Serializable
data class EstadoRequest(
    val usuario: String,
    val password: String,
    val tipo: String,
    val etiqueta: String,
)
