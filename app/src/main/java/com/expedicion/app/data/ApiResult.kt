package com.expedicion.app.data

/**
 * Resultado uniforme de una llamada al API. `Error.message` es SIEMPRE el texto que llego tal
 * cual en `error.message` del JSON de error del backend (fuente unica de verdad de textos), salvo
 * para fallas puramente de transporte (sin respuesta del servidor: sin red, timeout, host
 * inalcanzable) donde no existe un mensaje del backend para mostrar — ahi se usa un texto
 * generico client-side (`NETWORK_ERROR_MESSAGE`), unico texto de error que no proviene del API.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: String? = null, val httpStatus: Int? = null) : ApiResult<Nothing>()
}

const val NETWORK_ERROR_MESSAGE = "No se pudo conectar con el servidor. Verifique su conexión de red."

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Error -> this
}

/**
 * Un error HTTP >= 500 (falla de infraestructura/servidor) o una falla pura de transporte
 * (httpStatus null: sin red, timeout, host inalcanzable) no debe limpiar el estado de pantalla.
 * Replica el chequeo `ModuloDatos.FDConnection.Connected` de UnitIngreso.pas (y equivalentes en
 * despacho/devolucion): sin conexion al backend, se preserva lo que el usuario tenia cargado.
 */
fun ApiResult.Error.isServerFault(): Boolean = httpStatus == null || httpStatus >= 500
