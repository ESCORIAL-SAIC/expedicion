package com.expedicion.app.data.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.apiConfigDataStore by preferencesDataStore(name = "api_config")

/**
 * Persiste la URL base del API elegida por el usuario en runtime, reemplazando el valor
 * fijo de `BuildConfig.API_BASE_URL` inyectado en build-time. Sobrevive a reinicios de la app.
 */
@Singleton
class ApiConfigStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY_API_BASE_URL = stringPreferencesKey("api_base_url")
    private val KEY_API_VERSION = stringPreferencesKey("api_version")

    /** Null mientras no se haya configurado nunca una URL (primer arranque). */
    val apiBaseUrl: Flow<String?> = context.apiConfigDataStore.data.map { it[KEY_API_BASE_URL] }

    /**
     * Version que reporto `GET /version` la ultima vez que se valido la URL con exito. Se guarda
     * junto a la URL para poder mostrarla en Login sin volver a consultar el API en cada arranque.
     * Puede quedar desactualizada si el servidor se actualiza sin reconfigurar la app.
     */
    val apiVersion: Flow<String?> = context.apiConfigDataStore.data.map { it[KEY_API_VERSION] }

    suspend fun setApiBaseUrl(url: String, apiVersion: String) {
        context.apiConfigDataStore.edit {
            it[KEY_API_BASE_URL] = url
            it[KEY_API_VERSION] = apiVersion
        }
    }

    /**
     * Lectura bloqueante usada únicamente por el grafo de Hilt (`NetworkModule`) al construir el
     * `Retrofit` singleton, que necesita un `baseUrl` sincrónico en el momento de la creación.
     * La app garantiza (ver `ExpedicionNavGraph`) que nunca se llega a una pantalla que dispare
     * requests de red sin una URL ya persistida.
     */
    fun readBlocking(): String? = runBlocking { apiBaseUrl.first() }
}
