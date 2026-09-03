package com.expedicion.app.ui.common

import com.expedicion.app.data.config.ApiProbe

/**
 * Etiqueta legible de la version del API.
 *
 * El prefijo "v" se agrega solo a versiones que parecen un numero de version: `GET /version`
 * devuelve `"dev"` cuando el API corre sin `APP_VERSION` inyectada (p.ej. `npm run dev`, ver
 * `api/src/app.ts`), y ahi "API vdev" se leeria mal.
 */
fun apiVersionLabel(apiVersion: String?): String = when {
    apiVersion.isNullOrBlank() -> "API sin verificar"
    apiVersion == ApiProbe.VERSION_DESCONOCIDA -> "API (versión desconocida)"
    apiVersion.first().isDigit() -> "API v$apiVersion"
    else -> "API $apiVersion"
}

/**
 * Pie de versiones compartido por Login y Configuración, para que ambas pantallas digan lo mismo.
 *
 * `apiVersion` es la que reportó `GET /version` la última vez que se verificó la URL (persistida
 * junto a ella en `ApiConfigStore`); es null si nunca se verificó una conexión, y puede quedar
 * desactualizada si el servidor se actualiza sin reconfigurar la app.
 */
fun versionesTexto(appVersion: String, apiVersion: String?): String =
    "App v$appVersion · ${apiVersionLabel(apiVersion)}"
