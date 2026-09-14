package com.expedicion.app.data.circuito

/**
 * Circuitos de despacho de productos que no se fabrican en la linea de Escorial.
 *
 * Espeja modules/circuitos/config.ts de la API: el `slug` es el segmento de la ruta HTTP y el
 * `tipo` el valor de TIPO en la base. El servidor toma el TIPO de su propia config y no del
 * body, asi que el de aca solo se usa para mostrarlo y para armar el request de forma
 * consistente.
 *
 * Estos circuitos van por rutas propias (/importado, /peabody) y no por /despacho porque el
 * maestro de etiquetas es otro y las validaciones de unicidad no son las mismas: PEABODY no
 * maneja numeros de serie, tiene EANs que se repiten entre unidades del mismo producto.
 */
enum class Circuito(
    val slug: String,
    val tipo: String,
    val titulo: String,
) {
    IMPORTADO(slug = "importado", tipo = "IMPORT", titulo = "Importados"),
    PEABODY(slug = "peabody", tipo = "PEABODY", titulo = "Peabody");

    companion object {
        /**
         * Circuito que corresponde al TIPO de un remito, o `null` si va por el despacho clasico
         * (COCINA / TERMOTANQUE, y cualquier tipo nuevo que aparezca sin circuito propio).
         *
         * Esta resolucion es lo que permite que el operario entre siempre por Despacho, elija el
         * remito y la app decida sola contra que endpoint hablar: el tipo ya viene en la fila del
         * listado, asi que no hace falta que elija el circuito de antemano.
         */
        fun fromTipo(tipo: String): Circuito? =
            entries.firstOrNull { it.tipo.equals(tipo, ignoreCase = true) }
    }
}
