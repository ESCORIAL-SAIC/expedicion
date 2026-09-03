package com.expedicion.app.data.remote

import com.expedicion.app.data.ApiResult
import com.expedicion.app.data.NETWORK_ERROR_MESSAGE
import com.expedicion.app.data.remote.dto.ErrorEnvelopeDto
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Ejecuta una llamada Retrofit y la traduce a ApiResult, decodificando el body de error
 * `{ error: { code, message } }` tal cual lo define el API (nunca se reprocesa el texto).
 */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Error(NETWORK_ERROR_MESSAGE, httpStatus = response.code())
            }
        } else {
            val errorBody = response.errorBody()?.string()
            val parsed = errorBody?.let {
                runCatching { errorJson.decodeFromString<ErrorEnvelopeDto>(it) }.getOrNull()
            }
            if (parsed != null) {
                ApiResult.Error(parsed.error.message, parsed.error.code, response.code())
            } else {
                ApiResult.Error(NETWORK_ERROR_MESSAGE, httpStatus = response.code())
            }
        }
    } catch (e: IOException) {
        ApiResult.Error(NETWORK_ERROR_MESSAGE)
    } catch (e: SerializationException) {
        ApiResult.Error(NETWORK_ERROR_MESSAGE)
    }
}
