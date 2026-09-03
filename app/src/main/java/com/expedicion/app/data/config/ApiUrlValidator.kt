package com.expedicion.app.data.config

import java.net.URI

/** Resultado puro (sin dependencias de Android) de validar la URL ingresada en la pantalla de configuración. */
sealed class ApiUrlValidationResult {
    data class Valid(val normalizedUrl: String) : ApiUrlValidationResult()
    data class Invalid(val message: String) : ApiUrlValidationResult()
}

/**
 * Valida el formato de la URL base del API. No verifica conectividad real: eso lo maneja
 * cada pantalla con sus propios errores de red al hacer el primer request.
 */
object ApiUrlValidator {

    fun validate(input: String): ApiUrlValidationResult {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return ApiUrlValidationResult.Invalid("La URL no puede estar vacía.")
        }

        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            return ApiUrlValidationResult.Invalid("La URL ingresada no es válida.")
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return ApiUrlValidationResult.Invalid("La URL debe comenzar con http:// o https://")
        }
        if (uri.host.isNullOrBlank()) {
            return ApiUrlValidationResult.Invalid("La URL ingresada no es válida.")
        }

        val schemeIdx = trimmed.indexOf("://")
        val schemeNormalized = if (schemeIdx >= 0) scheme + trimmed.substring(schemeIdx) else trimmed

        val queryIdx = schemeNormalized.indexOf("?")
        val normalized = if (queryIdx >= 0) {
            val beforeQuery = schemeNormalized.substring(0, queryIdx)
            val fromQuery = schemeNormalized.substring(queryIdx)
            val path = if (beforeQuery.endsWith("/")) beforeQuery else "$beforeQuery/"
            path + fromQuery
        } else if (schemeNormalized.endsWith("/")) {
            schemeNormalized
        } else {
            "$schemeNormalized/"
        }
        return ApiUrlValidationResult.Valid(normalized)
    }
}
