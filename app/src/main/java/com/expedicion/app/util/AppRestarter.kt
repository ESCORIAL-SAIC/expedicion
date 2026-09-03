package com.expedicion.app.util

import android.content.Context
import android.content.Intent

/**
 * Reinicia el proceso completo de la app.
 *
 * Retrofit/OkHttp se construyen como singletons de Hilt (`NetworkModule`) leyendo la URL base una
 * sola vez, al crear el `SingletonComponent`. Ese componente vive en `Application` y sobrevive a
 * recrear una `Activity` (`recreate()` no alcanza), así que la única forma simple de que un cambio
 * de URL en `ApiConfigScreen` se refleje en los siguientes requests es matar el proceso y volver a
 * lanzar la Activity principal en uno nuevo: eso fuerza a que `Application`/Hilt y por lo tanto
 * `NetworkModule` se reconstruyan desde cero releyendo el valor ya persistido.
 */
object AppRestarter {

    fun restart(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = launchIntent?.component
        val restartIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(restartIntent)
        Runtime.getRuntime().exit(0)
    }
}
