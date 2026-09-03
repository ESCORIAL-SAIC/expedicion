package com.expedicion.app.sound

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feedback sonoro de exito/error al escanear.
 *
 * El Delphi original define este feedback via `Sound(1)`/`Sound(3)` en `UnitFunciones.pas`,
 * pero la funcion `Sound` esta deshabilitada alli por un `if False`: en la practica nunca
 * suena hoy en produccion. Para replicar el comportamiento REAL actual (no el codigo muerto),
 * `playSuccess`/`playError` quedan como no-op: se siguen invocando desde los ViewModels en los
 * mismos puntos del flujo (y por eso los tests de invocacion no cambian), pero no emiten audio.
 * El ToneGenerator queda instanciado y listo para usarse: si en el futuro se decide reactivar
 * el sonido (revertir el equivalente al `if False`), basta descomentar las llamadas a
 * `startTone` dentro de cada metodo.
 */
@Singleton
class SoundFeedbackPlayer @Inject constructor() {

    private val toneGenerator: ToneGenerator by lazy {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME_PERCENT)
    }

    fun playSuccess() {
        // Sonido deshabilitado: ver comentario de clase (equivalente al `if False` del Delphi).
        // runCatching { toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, SUCCESS_DURATION_MS) }
    }

    fun playError() {
        // Sonido deshabilitado: ver comentario de clase (equivalente al `if False` del Delphi).
        // runCatching { toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, ERROR_DURATION_MS) }
    }

    fun release() {
        runCatching { toneGenerator.release() }
    }

    private companion object {
        const val VOLUME_PERCENT = 80
        const val SUCCESS_DURATION_MS = 150
        const val ERROR_DURATION_MS = 350
    }
}
