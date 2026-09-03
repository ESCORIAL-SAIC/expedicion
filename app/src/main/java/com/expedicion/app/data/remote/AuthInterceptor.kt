package com.expedicion.app.data.remote

import com.expedicion.app.session.SessionManager
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Inyecta Authorization: Basic en cada request usando las credenciales vivas en SessionManager.
 * Si no hay sesion (p.ej. la propia llamada a /auth/login), el request sale sin header y el
 * servidor resuelve por body en ese caso.
 */
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val credentials = sessionManager.current() ?: return chain.proceed(original)

        val authenticated = original.newBuilder()
            .header("Authorization", Credentials.basic(credentials.usuario, credentials.password))
            .build()
        return chain.proceed(authenticated)
    }
}
