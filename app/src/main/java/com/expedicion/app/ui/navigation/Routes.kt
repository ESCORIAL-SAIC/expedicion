package com.expedicion.app.ui.navigation

object Routes {
    const val API_CONFIG = "api_config"
    const val LOGIN = "login"
    const val MENU = "menu"
    const val DESPACHO = "despacho"
    const val DEVOLUCION = "devolucion"
    const val ESTADO = "estado"

    // Circuitos IMPORT / PEABODY. Rutas fijas por circuito y no una con argumento, para
    // mantener el estilo del resto del grafo (strings planos, sin navArgument).
    const val IMPORTADO = "circuito_importado"
    const val PEABODY = "circuito_peabody"
}
