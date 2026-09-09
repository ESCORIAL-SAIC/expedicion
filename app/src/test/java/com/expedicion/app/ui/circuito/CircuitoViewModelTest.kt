package com.expedicion.app.ui.circuito

import com.expedicion.app.MainDispatcherRule
import com.expedicion.app.data.ApiResult
import com.expedicion.app.data.circuito.Circuito
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

class CircuitoViewModelTest {

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
        tipo = "PEABODY",
    )

    // EAN de 13 digitos: el caso real de Peabody, y de paso cubre que no se trate como int.
    private val ean = "7791234567890"

    private fun crearViewModel(circuito: Circuito = Circuito.PEABODY): CircuitoViewModel {
        coEvery { remitoRepository.detalle(any(), any()) } returns
            ApiResult.Success(DetalleRemitoResponseDto(items = emptyList(), totalEscaneado = 0))
        val viewModel = CircuitoViewModel(remitoRepository, escaneoRepository, soundPlayer)
        viewModel.inicializar(circuito)
        return viewModel
    }

    private fun crearViewModelConRemito(circuito: Circuito = Circuito.PEABODY): CircuitoViewModel {
        val viewModel = crearViewModel(circuito)
        viewModel.seleccionarRemito(remito)
        return viewModel
    }

    @Test
    fun `inicializar fija el circuito y es idempotente`() = runTest {
        val viewModel = crearViewModel(Circuito.IMPORTADO)
        assertEquals(Circuito.IMPORTADO, viewModel.uiState.value.circuito)

        // Un segundo llamado (recomposicion) no debe cambiar el circuito ya fijado.
        viewModel.inicializar(Circuito.PEABODY)
        assertEquals(Circuito.IMPORTADO, viewModel.uiState.value.circuito)
    }

    @Test
    fun `escanear usa el circuito de la pantalla`() = runTest {
        val viewModel = crearViewModelConRemito(Circuito.IMPORTADO)
        coEvery { escaneoRepository.escanearCircuito(any(), any(), any(), any()) } returns
            ApiResult.Success(ScanOutcome.Success("Producto Importado", 1, 4, 1))

        viewModel.onEtiquetaChange("12345")
        viewModel.escanear()

        coVerify {
            escaneoRepository.escanearCircuito(Circuito.IMPORTADO, "remito-1", "12345", "R-0001")
        }
    }

    // El caso que justifica el circuito aparte: Peabody no maneja numeros de serie, el mismo EAN
    // llega en todas las unidades. Las dos lecturas deben sumar, no descartarse como duplicado.
    //
    // Nota sobre el total: tras cada escaneo el ViewModel llama cargarDetalle(), cuyo
    // totalEscaneado sobrescribe el que vino en la respuesta del escaneo. Por eso el mock del
    // detalle tiene que devolver totales coherentes con la secuencia, o el assert mide el mock
    // del detalle y no el del escaneo.
    @Test
    fun `el mismo codigo escaneado dos veces suma las dos lecturas`() = runTest {
        coEvery { remitoRepository.detalle(any(), any()) } returnsMany listOf(
            ApiResult.Success(DetalleRemitoResponseDto(items = emptyList(), totalEscaneado = 0)),
            ApiResult.Success(DetalleRemitoResponseDto(items = emptyList(), totalEscaneado = 1)),
            ApiResult.Success(DetalleRemitoResponseDto(items = emptyList(), totalEscaneado = 2)),
        )
        val viewModel = CircuitoViewModel(remitoRepository, escaneoRepository, soundPlayer)
        viewModel.inicializar(Circuito.PEABODY)
        viewModel.seleccionarRemito(remito)

        coEvery { escaneoRepository.escanearCircuito(any(), any(), any(), any()) } returnsMany listOf(
            ApiResult.Success(ScanOutcome.Success("Producto Peabody", 1, 4, 1)),
            ApiResult.Success(ScanOutcome.Success("Producto Peabody", 2, 3, 2)),
        )

        viewModel.onEtiquetaChange(ean)
        viewModel.escanear()
        assertEquals(1, viewModel.uiState.value.totalEscaneado)

        viewModel.onEtiquetaChange(ean)
        viewModel.escanear()
        assertEquals(2, viewModel.uiState.value.totalEscaneado)
        assertNull(viewModel.uiState.value.errorMessage)

        // Lo esencial: dos altas, ninguna descartada como duplicado.
        coVerify(exactly = 2) { escaneoRepository.escanearCircuito(any(), any(), any(), any()) }
    }

    @Test
    fun `escaneo exitoso limpia el campo y muestra el producto`() = runTest {
        val viewModel = crearViewModelConRemito()
        coEvery { escaneoRepository.escanearCircuito(any(), any(), any(), any()) } returns
            ApiResult.Success(ScanOutcome.Success("Producto Peabody", 1, 4, 1))

        viewModel.onEtiquetaChange(ean)
        viewModel.escanear()

        val state = viewModel.uiState.value
        assertEquals("", state.etiquetaInput)
        assertEquals("Producto Peabody", state.ultimoProductoEscaneado)
        assertFalse(state.isLoading)
    }

    @Test
    fun `error de negocio limpia el campo y muestra el mensaje`() = runTest {
        val viewModel = crearViewModelConRemito()
        coEvery { escaneoRepository.escanearCircuito(any(), any(), any(), any()) } returns
            ApiResult.Error("Se ha alcanzado el total del item a remitir.", httpStatus = 422)

        viewModel.onEtiquetaChange(ean)
        viewModel.escanear()

        val state = viewModel.uiState.value
        assertEquals("Se ha alcanzado el total del item a remitir.", state.errorMessage)
        assertEquals("", state.etiquetaInput)
    }

    // Ante una falla de infraestructura el operador tiene que poder reintentar sin volver a
    // escanear: se preserva el campo y el remito.
    @Test
    fun `falla de servidor preserva el campo y el contexto`() = runTest {
        val viewModel = crearViewModelConRemito()
        coEvery { escaneoRepository.escanearCircuito(any(), any(), any(), any()) } returns
            ApiResult.Error("No se pudo conectar con el servidor.", httpStatus = null)

        viewModel.onEtiquetaChange(ean)
        viewModel.escanear()

        val state = viewModel.uiState.value
        assertEquals(ean, state.etiquetaInput)
        assertEquals("remito-1", state.remitoId)
    }

    @Test
    fun `escanear sin remito seleccionado no llama al repositorio`() = runTest {
        val viewModel = crearViewModel()

        viewModel.onEtiquetaChange(ean)
        viewModel.escanear()

        coVerify(exactly = 0) { escaneoRepository.escanearCircuito(any(), any(), any(), any()) }
    }

    @Test
    fun `buscarRemito autoselecciona cuando hay exactMatch`() = runTest {
        coEvery { remitoRepository.detalle(any(), any()) } returns
            ApiResult.Success(DetalleRemitoResponseDto(items = emptyList(), totalEscaneado = 0))
        coEvery { remitoRepository.listarCircuito(any(), any()) } returns
            ApiResult.Success(RemitoListResponseDto(exactMatch = remito, items = listOf(remito)))
        val viewModel = crearViewModel()

        viewModel.buscarRemito("R-0001")

        val state = viewModel.uiState.value
        assertEquals("remito-1", state.remitoId)
        assertFalse(state.mostrarBuscador)
    }

    @Test
    fun `abrirBuscador nunca autoselecciona aunque haya exactMatch`() = runTest {
        coEvery { remitoRepository.listarCircuito(any(), any()) } returns
            ApiResult.Success(RemitoListResponseDto(exactMatch = remito, items = listOf(remito)))
        val viewModel = crearViewModel()

        viewModel.abrirBuscador()

        val state = viewModel.uiState.value
        assertTrue(state.mostrarBuscador)
        assertNull(state.remitoId)
    }

    @Test
    fun `buscarRemito consulta el listado del circuito`() = runTest {
        coEvery { remitoRepository.listarCircuito(any(), any()) } returns
            ApiResult.Success(RemitoListResponseDto(exactMatch = null, items = emptyList()))
        val viewModel = crearViewModel(Circuito.PEABODY)

        viewModel.buscarRemito("R-0001")

        coVerify { remitoRepository.listarCircuito(Circuito.PEABODY, "R-0001") }
    }

    @Test
    fun `eliminar etiqueta vacia no llama al repositorio`() = runTest {
        val viewModel = crearViewModelConRemito()

        viewModel.eliminarEtiqueta("   ")

        coVerify(exactly = 0) { escaneoRepository.eliminarEtiquetaCircuito(any(), any(), any()) }
    }

    // Igual que en el escaneo, cargarDetalle() corre despues y sobrescribe el total, asi que el
    // mock del detalle tiene que acompaniar.
    @Test
    fun `eliminar etiqueta actualiza el total`() = runTest {
        coEvery { remitoRepository.detalle(any(), any()) } returnsMany listOf(
            ApiResult.Success(DetalleRemitoResponseDto(items = emptyList(), totalEscaneado = 4)),
            ApiResult.Success(DetalleRemitoResponseDto(items = emptyList(), totalEscaneado = 3)),
        )
        val viewModel = CircuitoViewModel(remitoRepository, escaneoRepository, soundPlayer)
        viewModel.inicializar(Circuito.PEABODY)
        viewModel.seleccionarRemito(remito)

        coEvery { escaneoRepository.eliminarEtiquetaCircuito(any(), any(), any()) } returns
            ApiResult.Success(EliminarResponseDto(success = true, totalEscaneado = 3))

        viewModel.eliminarEtiqueta(ean)

        assertEquals(3, viewModel.uiState.value.totalEscaneado)
        coVerify { escaneoRepository.eliminarEtiquetaCircuito(Circuito.PEABODY, "remito-1", ean) }
    }

    @Test
    fun `confirmar exitoso cierra la pantalla`() = runTest {
        val viewModel = crearViewModelConRemito()
        coEvery { escaneoRepository.confirmarCircuito(any(), any()) } returns ApiResult.Success(Unit)

        viewModel.confirmar()

        assertTrue(viewModel.uiState.value.cerrarPantalla)
    }

    @Test
    fun `confirmar con cantidades que difieren no cierra y muestra el error`() = runTest {
        val viewModel = crearViewModelConRemito()
        coEvery { escaneoRepository.confirmarCircuito(any(), any()) } returns
            ApiResult.Error(
                "Existen items que difieren de la cantidad original a remitir. Proceso cancelado.",
                httpStatus = 422,
            )

        viewModel.confirmar()

        val state = viewModel.uiState.value
        assertFalse(state.cerrarPantalla)
        assertEquals(
            "Existen items que difieren de la cantidad original a remitir. Proceso cancelado.",
            state.errorMessage,
        )
    }

    @Test
    fun `borrar transaccion exitoso cierra la pantalla y preserva el circuito`() = runTest {
        val viewModel = crearViewModelConRemito(Circuito.IMPORTADO)
        coEvery { escaneoRepository.borrarTransaccionCircuito(any(), any()) } returns
            ApiResult.Success(Unit)

        viewModel.solicitarBorrarTransaccion()
        viewModel.confirmarBorrarTransaccion()

        val state = viewModel.uiState.value
        assertTrue(state.cerrarPantalla)
        // El reset no debe perder el circuito: la pantalla seguiria viva apuntando a otro.
        assertEquals(Circuito.IMPORTADO, state.circuito)
        assertNull(state.remitoId)
    }

    @Test
    fun `solicitar borrar transaccion sin remito no abre el dialogo`() = runTest {
        val viewModel = crearViewModel()

        viewModel.solicitarBorrarTransaccion()

        assertFalse(viewModel.uiState.value.mostrarConfirmarBorrarTransaccion)
    }
}
