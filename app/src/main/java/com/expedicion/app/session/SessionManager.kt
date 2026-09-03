package com.expedicion.app.session

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credenciales del usuario logueado, vivas solo en memoria de proceso. Nunca se persisten
 * en disco (SharedPreferences, DataStore, DB local, etc.): si el proceso muere, se pierden
 * y hay que volver a loguearse. Cada operacion sensible las vuelve a leer de aca (no hay
 * "sesion validada" cacheada: el API revalida usuario+password en cada request).
 */
@Singleton
class SessionManager @Inject constructor() {

    data class Credentials(val usuario: String, val password: String)

    @Volatile
    private var credentials: Credentials? = null

    fun setCredentials(usuario: String, password: String) {
        credentials = Credentials(usuario, password)
    }

    fun current(): Credentials? = credentials

    fun isLoggedIn(): Boolean = credentials != null

    fun clear() {
        credentials = null
    }
}
