package com.expedicion.app.data.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

/** Resultado de sondear una URL candidata antes de persistirla. */
sealed class ApiProbeResult {
    /**
     * El API respondio y ambas bases estan OK. `version` es lo que devolvio `GET /version`
     * (`"dev"` cuando el API corre sin `APP_VERSION` inyectada, p.ej. `npm run dev`).
     */
    data class Ok(val version: String) : ApiProbeResult()

    /** No hubo respuesta HTTP: host inalcanzable, timeout, DNS, puerto cerrado. */
    data object Unreachable : ApiProbeResult()

    /**
     * Respondio HTTP pero no es un API de Expedicion sano: status inesperado en `/health/ready`
     * o un body que no se pudo interpretar. `detalle` describe lo observado.
     */
    data class NotHealthy(val detalle: String) : ApiProbeResult()

    /**
     * El API responde pero `/health/ready` da 503: alguna base no contesta. Se distingue de
     * `NotHealthy` porque aca si sabemos cual falla y el problema no es la URL.
     */
    data class DbUnavailable(val postgresOk: Boolean, val mssqlOk: Boolean) : ApiProbeResult()
}

@Serializable
private data class HealthReadyDto(
    val status: String? = null,
    val checks: HealthChecksDto? = null,
)

@Serializable
private data class HealthChecksDto(
    val postgres: String? = null,
    val mssql: String? = null,
)

@Serializable
private data class VersionDto(val version: String? = null)

/**
 * Sondea una URL base candidata contra los endpoints publicos del API (`/health/ready` y
 * `/version`, ver `api/src/app.ts`), sin pasar por Retrofit ni por `AuthInterceptor`.
 *
 * Deliberadamente NO reusa el `Retrofit`/`ApiService` inyectado: ese singleton tiene su `baseUrl`
 * fijada al construirse el grafo de Hilt (ver `NetworkModule`), mientras que aca hay que consultar
 * una URL que el usuario acaba de tipear y que todavia no se persistio. Usa su propio
 * `OkHttpClient` con timeouts cortos para no dejar la pantalla colgada.
 */
@Singleton
class ApiProbe @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /** @param baseUrl URL ya normalizada por `ApiUrlValidator` (con `/` final). */
    suspend fun probe(baseUrl: String): ApiProbeResult = withContext(Dispatchers.IO) {
        val ready = get("${baseUrl}health/ready") ?: return@withContext ApiProbeResult.Unreachable

        val readyDto = ready.body?.let { runCatching { json.decodeFromString<HealthReadyDto>(it) }.getOrNull() }

        when {
            ready.code == 200 && readyDto?.status == "ok" -> ApiProbeResult.Ok(leerVersion(baseUrl))

            ready.code == 503 && readyDto?.checks != null -> ApiProbeResult.DbUnavailable(
                postgresOk = readyDto.checks.postgres == "ok",
                mssqlOk = readyDto.checks.mssql == "ok",
            )

            // Contesto algo, pero no el contrato esperado: tipicamente otra cosa escuchando en
            // ese host/puerto (un proxy, otra app, un portal de login de red).
            else -> ApiProbeResult.NotHealthy("el servidor respondió HTTP ${ready.code}")
        }
    }

    /**
     * La version es informativa: si `/version` falla o trae un body raro no se invalida una URL
     * cuyo `/health/ready` ya dio OK, se reporta como desconocida.
     */
    private fun leerVersion(baseUrl: String): String {
        val response = get("${baseUrl}version") ?: return VERSION_DESCONOCIDA
        if (response.code != 200) return VERSION_DESCONOCIDA
        val version = response.body
            ?.let { runCatching { json.decodeFromString<VersionDto>(it) }.getOrNull() }
            ?.version
        return version?.takeIf { it.isNotBlank() } ?: VERSION_DESCONOCIDA
    }

    private fun get(url: String): RawResponse? = try {
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            RawResponse(response.code, response.body?.string())
        }
    } catch (e: IOException) {
        null
    } catch (e: IllegalArgumentException) {
        // OkHttp rechaza la URL aunque URI la haya aceptado (ver ApiUrlValidator).
        null
    }

    private data class RawResponse(val code: Int, val body: String?)

    companion object {
        const val VERSION_DESCONOCIDA = "desconocida"
        private const val PROBE_TIMEOUT_SECONDS = 8L
    }
}
