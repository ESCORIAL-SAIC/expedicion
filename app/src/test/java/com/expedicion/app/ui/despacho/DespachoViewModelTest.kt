package com.expedicion.app.ui.despacho

import com.expedicion.app.MainDispatcherRule
import com.expedicion.app.data.ApiResult
import com.expedicion.app.data.remote.dto.DetalleRemitoResponseDto
import com.expedicion.app.data.remote.dto.EliminarResponseDto
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

class DespachoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val remitoRepository = mockk<RemitoRepository>()
    private val escaneoRepository = mockk<EscaneoRepository>()
    private val soundPlayer = mockk<SoundFeedbackPlayer>(relaxed = true)

    private val remito = RemitoListItemDto(
        remitoN = "R-0001",
        clienteN = "Cliente Uno",
        remitoId = "remito-1",
        clienteId = "cliente-1",
        tipo = "COCINA",
    )

    private fun crearViewModelConRemitoSeleccionado(): DespachoViewModel {
        coEvery { remitoRepository.detalle(any(), any()) } returns
            ApiResult.Success(DetalleRemitoResponseDto(items = emptyList(), totalEscaneado = 0))
        val viewModel = DespachoViewModel(remitoRepository, escaneoRepository, soundPlayer)
        viewModel.seleccionarRemito(remito)
        return viewModel
    }

    @Test
    fun `contador cicla de 1 a 8 y vuelve a 1`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        coEvery { escaneoRepository.escanear(true, any(), any(), any(), any()) } returns
            ApiResult.Success(ScanOutcome.Success("Cocina X", 1, 1, 1))

        repeat(8) { viewModel.escanear() }
        assertEquals(8, viewModel.uiState.value.contador)

        viewModel.escanear()
        assertEquals(1, viewModel.uiState.value.contador)
    }

    @Test
    fun `resetContador vuelve el contador a 0`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        coEvery { escaneoRepository.escanear(true, any(), any(), any(), any()) } returns
            ApiResult.Success(ScanOutcome.Success("Cocina X", 1, 1, 1))

        viewModel.escanear()
        assertTrue(viewModel.uiState.value.contador > 0)

        viewModel.resetContador()
        assertEquals(0, viewModel.uiState.value.contador)
    }

    @Test
    fun `duplicado no muestra error ni sonido de exito, solo limpia el campo`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        viewModel.onEtiquetaChange("12345")
        coEvery { escaneoRepository.escanear(true, any(), any(), any(), any()) } returns
            ApiResult.Success(ScanOutcome.Duplicated)

        viewModel.escanear()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals("", state.etiquetaInput)
        assertEquals(0, state.contador)
    }

    @Test
    fun `error 503 muestra dialogo pero no resetea etiqueta ni contador`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        coEvery { escaneoRepository.escanear(true, any(), any(), any(), any()) } returns
            ApiResult.Success(ScanOutcome.Success("Cocina X", 1, 1, 1))
        viewModel.escanear()
        val contadorPrevio = viewModel.uiState.value.contador
        viewModel.onEtiquetaChange("99999")

        val mensaje503 = "No se pudo establecer conexión con la Base de Datos. Cierre la aplicación, revise su configuración de red y vuelva a intentar abrir la aplicación."
        coEvery { escaneoRepository.escanear(true, any(), any(), any(), any()) } returns
            ApiResult.Error(mensaje503, "DB_UNAVAILABLE", 503)

        viewModel.escanear()

        val state = viewModel.uiState.value
        assertEquals(mensaje503, state.errorMessage)
        assertEquals("99999", state.etiquetaInput)
        assertEquals(contadorPrevio, state.contador)
        assertEquals(remito.remitoId, state.remitoId)
    }

    @Test
    fun `error de negocio limpia el campo etiqueta`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        viewModel.onEtiquetaChange("99999")
        coEvery { escaneoRepository.escanear(true, any(), any(), any(), any()) } returns
            ApiResult.Error("El código 99999 no pertenece a un producto del remito.", "PRODUCT_NOT_IN_REMITO", 422)

        viewModel.escanear()

        val state = viewModel.uiState.value
        assertEquals("El código 99999 no pertenece a un producto del remito.", state.errorMessage)
        assertEquals("", state.etiquetaInput)
    }

    @Test
    fun `confirmar con diferencias muestra error y no cierra la pantalla`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        coEvery { escaneoRepository.confirmarDespacho(any()) } returns
            ApiResult.Error("Existen items que difieren de la cantidad original a remitir. Proceso cancelado.", "QUANTITY_MISMATCH", 422)

        viewModel.confirmar()

        val state = viewModel.uiState.value
        assertFalse(state.cerrarPantalla)
        assertEquals("Existen items que difieren de la cantidad original a remitir. Proceso cancelado.", state.errorMessage)
    }

    @Test
    fun `confirmar sin diferencias cierra la pantalla`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        coEvery { escaneoRepository.confirmarDespacho(any()) } returns ApiResult.Success(Unit)

        viewModel.confirmar()

        assertTrue(viewModel.uiState.value.cerrarPantalla)
    }

    @Test
    fun `duplicado no dispara ningun sonido, ni exito ni error`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        coEvery { escaneoRepository.escanear(true, any(), any(), any(), any()) } returns
            ApiResult.Success(ScanOutcome.Duplicated)

        viewModel.escanear()

        coVerify(exactly = 0) { soundPlayer.playSuccess() }
        coVerify(exactly = 0) { soundPlayer.playError() }
    }

    @Test
    fun `error de negocio dispara sonido de error pero nunca el de exito`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        coEvery { escaneoRepository.escanear(true, any(), any(), any(), any()) } returns
            ApiResult.Error("El código 1 no pertenece a un producto del remito.", "PRODUCT_NOT_IN_REMITO", 422)

        viewModel.escanear()

        coVerify(exactly = 1) { soundPlayer.playError() }
        coVerify(exactly = 0) { soundPlayer.playSuccess() }
    }

    @Test
    fun `eliminar etiqueta exitoso decrementa el contador en 1`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        coEvery { escaneoRepository.escanear(true, any(), any(), any(), any()) } returns
            ApiResult.Success(ScanOutcome.Success("Cocina X", 1, 1, 1))
        repeat(3) { viewModel.escanear() }
        assertEquals(3, viewModel.uiState.value.contador)

        coEvery { escaneoRepository.eliminarEtiqueta(true, any(), any(), any()) } returns
            ApiResult.Success(EliminarResponseDto(success = true, totalEscaneado = 2))

        viewModel.eliminarEtiqueta("12345")

        assertEquals(2, viewModel.uiState.value.contador)
    }

    @Test
    fun `eliminar etiqueta con texto vacio no llama al repository`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()

        viewModel.eliminarEtiqueta("")

        coVerify(exactly = 0) { escaneoRepository.eliminarEtiqueta(any(), any(), any(), any()) }
    }

    @Test
    fun `eliminar etiqueta con contador en 0 no baja del piso 0`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        assertEquals(0, viewModel.uiState.value.contador)
        coEvery { escaneoRepository.eliminarEtiqueta(true, any(), any(), any()) } returns
            ApiResult.Success(EliminarResponseDto(success = true, totalEscaneado = 0))

        viewModel.eliminarEtiqueta("12345")

        assertEquals(0, viewModel.uiState.value.contador)
    }

    @Test
    fun `buscarRemito con match exacto selecciona el remito sin abrir el buscador`() = runTest {
        coEvery { remitoRepository.detalle(any(), any()) } returns
            ApiResult.Success(DetalleRemitoResponseDto(items = emptyList(), totalEscaneado = 0))
        coEvery { remitoRepository.listarDespacho("R-0001") } returns
            ApiResult.Success(RemitoListResponseDto(exactMatch = remito, items = listOf(remito)))
        val viewModel = DespachoViewModel(remitoRepository, escaneoRepository, soundPlayer)

        viewModel.buscarRemito("R-0001")

        val state = viewModel.uiState.value
        assertFalse(state.mostrarBuscador)
        assertEquals(remito.remitoId, state.remitoId)
        assertEquals(remito.remitoN, state.remitoN)
    }

    @Test
    fun `buscarRemito sin match exacto abre el buscador con los candidatos y no selecciona remito`() = runTest {
        val candidatos = listOf(
            remito,
            remito.copy(remitoN = "R-0002", remitoId = "remito-2", clienteN = "Cliente Dos"),
        )
        coEvery { remitoRepository.listarDespacho("R-00") } returns
            ApiResult.Success(RemitoListResponseDto(exactMatch = null, items = candidatos))
        val viewModel = DespachoViewModel(remitoRepository, escaneoRepository, soundPlayer)

        viewModel.buscarRemito("R-00")

        val state = viewModel.uiState.value
        assertTrue(state.mostrarBuscador)
        assertEquals(candidatos, state.remitosCandidatos)
        assertNull(state.remitoId)
    }

    @Test
    fun `abrirBuscador lista todos los remitos con busqueda vacia y abre el buscador`() = runTest {
        val candidatos = listOf(
            remito,
            remito.copy(remitoN = "R-0002", remitoId = "remito-2", clienteN = "Cliente Dos"),
        )
        coEvery { remitoRepository.listarDespacho("") } returns
            ApiResult.Success(RemitoListResponseDto(exactMatch = null, items = candidatos))
        val viewModel = DespachoViewModel(remitoRepository, escaneoRepository, soundPlayer)

        viewModel.abrirBuscador()

        val state = viewModel.uiState.value
        assertTrue(state.mostrarBuscador)
        assertEquals(candidatos, state.remitosCandidatos)
        assertNull(state.remitoId)
        coVerify { remitoRepository.listarDespacho("") }
    }

    @Test
    fun `abrirBuscador nunca autoselecciona aunque el API devuelva exactMatch`() = runTest {
        // La lupa es "quiero elegir de la lista": si el backend devolviera un exactMatch (no deberia
        // con remitoN vacio, pero es contrato del server, no nuestro), igual tiene que abrir el
        // listado en vez de saltar directo a un remito que el usuario no eligio.
        coEvery { remitoRepository.listarDespacho("") } returns
            ApiResult.Success(RemitoListResponseDto(exactMatch = remito, items = listOf(remito)))
        val viewModel = DespachoViewModel(remitoRepository, escaneoRepository, soundPlayer)

        viewModel.abrirBuscador()

        val state = viewModel.uiState.value
        assertTrue(state.mostrarBuscador)
        assertNull(state.remitoId)
    }

    @Test
    fun `abrirBuscador con error del API muestra el mensaje y no abre el buscador`() = runTest {
        coEvery { remitoRepository.listarDespacho("") } returns
            ApiResult.Error(message = "Servidor caido", httpStatus = 500)
        val viewModel = DespachoViewModel(remitoRepository, escaneoRepository, soundPlayer)

        viewModel.abrirBuscador()

        val state = viewModel.uiState.value
        assertFalse(state.mostrarBuscador)
        assertEquals("Servidor caido", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `solicitarBorrarTransaccion sin remito seleccionado no muestra el dialogo`() = runTest {
        val viewModel = DespachoViewModel(remitoRepository, escaneoRepository, soundPlayer)

        viewModel.solicitarBorrarTransaccion()

        assertFalse(viewModel.uiState.value.mostrarConfirmarBorrarTransaccion)
    }

    @Test
    fun `confirmarBorrarTransaccion con error de API no cierra la pantalla y muestra el mensaje`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        viewModel.solicitarBorrarTransaccion()
        coEvery { escaneoRepository.borrarTransaccion(true, any()) } returns
            ApiResult.Error("No se pudo establecer conexión con la Base de Datos.", "DB_UNAVAILABLE", 503)

        viewModel.confirmarBorrarTransaccion()

        val state = viewModel.uiState.value
        assertFalse(state.cerrarPantalla)
        assertFalse(state.mostrarConfirmarBorrarTransaccion)
        assertEquals("No se pudo establecer conexión con la Base de Datos.", state.errorMessage)
    }

    /**
     * Acceptance criteria (item 5 de la spec de QA) y `UnitFunciones.pas` (`BorrarTransaccion`,
     * linea 841-844: `if es_despacho then FormDespacho.Close`): tras confirmar el borrado
     * irreversible y que el endpoint responda exito, la pantalla debe cerrarse. La implementacion
     * actual solo resetea el estado a un `DespachoUiState()` en blanco (remito null, items vacios)
     * pero nunca marca `cerrarPantalla = true`, por lo que `DespachoScreen` (que solo navega hacia
     * atras en el `LaunchedEffect(uiState.cerrarPantalla)`) no cierra la pantalla. Este test
     * documenta el criterio esperado y falla contra la implementacion actual.
     */
    @Test
    fun `confirmarBorrarTransaccion exitoso deberia cerrar la pantalla`() = runTest {
        val viewModel = crearViewModelConRemitoSeleccionado()
        viewModel.solicitarBorrarTransaccion()
        coEvery { escaneoRepository.borrarTransaccion(true, any()) } returns ApiResult.Success(Unit)

        viewModel.confirmarBorrarTransaccion()

        assertTrue("BUG: confirmarBorrarTransaccion exitoso no marca cerrarPantalla=true", viewModel.uiState.value.cerrarPantalla)
    }
}
