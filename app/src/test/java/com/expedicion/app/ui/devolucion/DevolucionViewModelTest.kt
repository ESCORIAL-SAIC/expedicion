package com.expedicion.app.ui.devolucion

import com.expedicion.app.MainDispatcherRule
import com.expedicion.app.data.ApiResult
import com.expedicion.app.data.remote.dto.DetalleRemitoResponseDto
import com.expedicion.app.data.remote.dto.RemitoListItemDto
import com.expedicion.app.data.remote.dto.RemitoListResponseDto
import com.expedicion.app.data.repository.EscaneoRepository
import com.expedicion.app.data.repository.RemitoRepository
import com.expedicion.app.data.repository.ScanOutcome
import com.expedicion.app.sound.SoundFeedbackPlayer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DevolucionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val remitoRepository = mockk<RemitoRepository>()
    private val escaneoRepository = mockk<EscaneoRepository>()
    private val soundPlayer = mockk<SoundFeedbackPlayer>(relaxed = true)

    private val remito = RemitoListItemDto(
        remitoN = "R-0003",
        clienteN = "Cliente Tres",
        remitoId = "remito-3",
        clienteId = "cliente-3",
        tipo = "COCINA",
    )

    private fun crearViewModelConRemitoSeleccionado(): DevolucionViewModel {
        coEvery { remitoRepository.detalle(any(), any()) } returns
            ApiResult.Success(DetalleRemitoResponseDto(items = emptyList()))
        val viewModel = DevolucionViewModel(remitoRepository, escaneoRepository, soundPlayer)
        viewModel.seleccionarRemito(remito)
        return viewModel
    }

    @Test
    fun `duplicado no muestra error, solo limpia el campo`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        viewModel.onEtiquetaChange("777")
        coEvery { escaneoRepository.escanear(false, any(), any(), any(), any()) } returns
            ApiResult.Success(ScanOutcome.Duplicated)

        viewModel.escanear()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals("", state.etiquetaInput)
    }

    @Test
    fun `confirmar cierra la pantalla aunque el API devuelva error (sin validar, igual al Delphi original)`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        coEvery { escaneoRepository.confirmarDevolucion(any()) } returns
            ApiResult.Error("Se ha producido un error al momento de registrar lectura. Reintente nuevamente.  boom", "SCAN_ERROR", 500)

        viewModel.confirmar()

        assertTrue(viewModel.uiState.value.cerrarPantalla)
    }

    @Test
    fun `confirmar cierra la pantalla cuando el API responde con exito`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        coEvery { escaneoRepository.confirmarDevolucion(any()) } returns ApiResult.Success(Unit)

        viewModel.confirmar()

        assertTrue(viewModel.uiState.value.cerrarPantalla)
    }

    @Test
    fun `duplicado no dispara ningun sonido`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        coEvery { escaneoRepository.escanear(false, any(), any(), any(), any()) } returns
            ApiResult.Success(ScanOutcome.Duplicated)

        viewModel.escanear()

        coVerify(exactly = 0) { soundPlayer.playSuccess() }
        coVerify(exactly = 0) { soundPlayer.playError() }
    }

    @Test
    fun `error 503 en escaneo muestra dialogo pero preserva el campo etiqueta y no reenfoca`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        viewModel.onEtiquetaChange("99999")
        val focusPrevio = viewModel.uiState.value.focusTrigger
        val mensaje503 = "No se pudo establecer conexión con la Base de Datos. Cierre la aplicación, revise su configuración de red y vuelva a intentar abrir la aplicación."
        coEvery { escaneoRepository.escanear(false, any(), any(), any(), any()) } returns
            ApiResult.Error(mensaje503, "DB_UNAVAILABLE", 503)

        viewModel.escanear()

        val state = viewModel.uiState.value
        assertEquals(mensaje503, state.errorMessage)
        assertEquals("99999", state.etiquetaInput)
        assertEquals(focusPrevio, state.focusTrigger)
        coVerify(exactly = 1) { soundPlayer.playError() }
    }

    @Test
    fun `error de negocio en escaneo limpia el campo etiqueta y reenfoca`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        viewModel.onEtiquetaChange("99999")
        val focusPrevio = viewModel.uiState.value.focusTrigger
        coEvery { escaneoRepository.escanear(false, any(), any(), any(), any()) } returns
            ApiResult.Error("El código 99999 no es válido. Reintente nuevamente.", "INVALID_LABEL", 404)

        viewModel.escanear()

        val state = viewModel.uiState.value
        assertEquals("El código 99999 no es válido. Reintente nuevamente.", state.errorMessage)
        assertEquals("", state.etiquetaInput)
        assertTrue(state.focusTrigger > focusPrevio)
    }

    @Test
    fun `buscarRemito con match exacto selecciona el remito sin abrir el buscador`() = runTest {
        coEvery { remitoRepository.detalle(any(), any()) } returns
            ApiResult.Success(DetalleRemitoResponseDto(items = emptyList()))
        coEvery { remitoRepository.listarDevolucion("R-0003") } returns
            ApiResult.Success(RemitoListResponseDto(exactMatch = remito, items = listOf(remito)))
        val viewModel = DevolucionViewModel(remitoRepository, escaneoRepository, soundPlayer)

        viewModel.buscarRemito("R-0003")

        val state = viewModel.uiState.value
        assertFalse(state.mostrarBuscador)
        assertEquals(remito.remitoId, state.remitoId)
    }

    @Test
    fun `buscarRemito sin match exacto abre el buscador con los candidatos`() = runTest {
        val candidatos = listOf(remito, remito.copy(remitoN = "R-0004", remitoId = "remito-4"))
        coEvery { remitoRepository.listarDevolucion("R-00") } returns
            ApiResult.Success(RemitoListResponseDto(exactMatch = null, items = candidatos))
        val viewModel = DevolucionViewModel(remitoRepository, escaneoRepository, soundPlayer)

        viewModel.buscarRemito("R-00")

        val state = viewModel.uiState.value
        assertTrue(state.mostrarBuscador)
        assertEquals(candidatos, state.remitosCandidatos)
        assertNull(state.remitoId)
    }

    @Test
    fun `confirmarBorrarTransaccion con error de API no cierra la pantalla`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        viewModel.solicitarBorrarTransaccion()
        coEvery { escaneoRepository.borrarTransaccion(false, any()) } returns
            ApiResult.Error("No se pudo establecer conexión con la Base de Datos.", "DB_UNAVAILABLE", 503)

        viewModel.confirmarBorrarTransaccion()

        val state = viewModel.uiState.value
        assertFalse(state.cerrarPantalla)
        assertEquals("No se pudo establecer conexión con la Base de Datos.", state.errorMessage)
    }

    /**
     * Igual que en Despacho (ver DespachoViewModelTest): `UnitFunciones.pas` `BorrarTransaccion`
     * cierra `FormDevolucion` tras el borrado exitoso, y `confirmarBorrarTransaccion` lo replica
     * reseteando con `DevolucionUiState(cerrarPantalla = true)` (DevolucionViewModel.kt:170).
     */
    @Test
    fun `confirmarBorrarTransaccion exitoso cierra la pantalla`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        viewModel.solicitarBorrarTransaccion()
        coEvery { escaneoRepository.borrarTransaccion(false, any()) } returns ApiResult.Success(Unit)

        viewModel.confirmarBorrarTransaccion()

        assertTrue(viewModel.uiState.value.cerrarPantalla)
    }
}
