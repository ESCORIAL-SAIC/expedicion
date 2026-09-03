package com.expedicion.app.ui.estado

import com.expedicion.app.MainDispatcherRule
import com.expedicion.app.data.ApiResult
import com.expedicion.app.data.repository.EstadoOutcome
import com.expedicion.app.data.repository.EstadoRepository
import com.expedicion.app.sound.SoundFeedbackPlayer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EstadoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val estadoRepository = mockk<EstadoRepository>()
    private val soundPlayer = mockk<SoundFeedbackPlayer>(relaxed = true)

    private fun crearViewModel(): EstadoViewModel = EstadoViewModel(estadoRepository, soundPlayer)

    @Test
    fun `abort silencioso (tipo vacio) no muestra error ni dispara sonido`() = runTest {
        val viewModel = crearViewModel()
        viewModel.onTipoChange("")
        viewModel.onEtiquetaChange("12345")
        coEvery { estadoRepository.consultar(any(), any()) } returns ApiResult.Success(EstadoOutcome.Aborted)

        viewModel.consultar()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertNull(state.resultado)
        coVerify(exactly = 0) { soundPlayer.playError() }
    }

    @Test
    fun `consulta exitosa disponible limpia el campo etiqueta y reenfoca`() = runTest {
        val viewModel = crearViewModel()
        viewModel.onTipoChange("COCINA")
        viewModel.onEtiquetaChange("12345")
        val focusPrevio = viewModel.uiState.value.focusTrigger
        coEvery { estadoRepository.consultar("COCINA", "12345") } returns
            ApiResult.Success(EstadoOutcome.Disponible("Cocina X"))

        viewModel.consultar()

        val state = viewModel.uiState.value
        assertEquals(EstadoOutcome.Disponible("Cocina X"), state.resultado)
        assertEquals("", state.etiquetaInput)
        assertTrue(state.focusTrigger > focusPrevio)
        coVerify(exactly = 0) { soundPlayer.playError() }
    }

    @Test
    fun `consulta exitosa no disponible informa cliente, remito y fecha del ultimo despacho`() = runTest {
        val viewModel = crearViewModel()
        viewModel.onTipoChange("TERMOTANQUE")
        viewModel.onEtiquetaChange("999")
        coEvery { estadoRepository.consultar("TERMOTANQUE", "999") } returns
            ApiResult.Success(
                EstadoOutcome.NoDisponible(
                    clienteN = "Cliente Uno",
                    productoN = "Termotanque X",
                    remitoN = "R-0099",
                    fechaHora = "2026-08-18 10:00",
                ),
            )

        viewModel.consultar()

        val resultado = viewModel.uiState.value.resultado
        assertEquals(
            EstadoOutcome.NoDisponible("Cliente Uno", "Termotanque X", "R-0099", "2026-08-18 10:00"),
            resultado,
        )
    }

    @Test
    fun `error del API dispara sonido de error, limpia el campo y reenfoca`() = runTest {
        val viewModel = crearViewModel()
        viewModel.onTipoChange("COCINA")
        viewModel.onEtiquetaChange("77777")
        val focusPrevio = viewModel.uiState.value.focusTrigger
        coEvery { estadoRepository.consultar(any(), any()) } returns
            ApiResult.Error("El código 77777 no existe en la base de datos.", "LABEL_NOT_FOUND", 404)

        viewModel.consultar()

        val state = viewModel.uiState.value
        assertEquals("El código 77777 no existe en la base de datos.", state.errorMessage)
        assertEquals("", state.etiquetaInput)
        assertTrue(state.focusTrigger > focusPrevio)
        coVerify(exactly = 1) { soundPlayer.playError() }
    }
}
