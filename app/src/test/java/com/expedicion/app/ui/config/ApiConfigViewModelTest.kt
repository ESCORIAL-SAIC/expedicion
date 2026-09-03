package com.expedicion.app.ui.config

import com.expedicion.app.MainDispatcherRule
import com.expedicion.app.data.config.ApiConfigStore
import com.expedicion.app.data.config.ApiProbe
import com.expedicion.app.data.config.ApiProbeResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ApiConfigViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiConfigStore = mockk<ApiConfigStore>()
    private val apiProbe = mockk<ApiProbe>()

    @Test
    fun `sin url guardada arranca en modo configuracion inicial`() = runTest {
        givenStore(url = null)
        val viewModel = ApiConfigViewModel(apiConfigStore, apiProbe)

        val state = viewModel.uiState.value
        assertTrue(state.isInitialSetup)
        assertFalse(state.isLoading)
    }

    @Test
    fun `con url guardada no es configuracion inicial y precarga el input con esa url`() = runTest {
        givenStore(url = "https://servidor-actual.com/", version = "1.2.3")
        val viewModel = ApiConfigViewModel(apiConfigStore, apiProbe)

        val state = viewModel.uiState.value
        assertFalse(state.isInitialSetup)
        assertEquals("https://servidor-actual.com/", state.urlInput)
        assertEquals("1.2.3", state.apiVersion)
    }

    @Test
    fun `guardar con url invalida no persiste, muestra error y no sondea el API`() = runTest {
        givenStore(url = null)
        val viewModel = ApiConfigViewModel(apiConfigStore, apiProbe)
        viewModel.onUrlChange("no-es-una-url")

        viewModel.guardar()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.guardadoExitoso)
        coVerify(exactly = 0) { apiProbe.probe(any()) }
        coVerify(exactly = 0) { apiConfigStore.setApiBaseUrl(any(), any()) }
    }

    @Test
    fun `guardar con url alcanzable persiste url y version y marca guardado exitoso`() = runTest {
        givenStore(url = null)
        coEvery { apiProbe.probe("http://192.168.0.10:3000/") } returns ApiProbeResult.Ok("1.4.0")
        coEvery { apiConfigStore.setApiBaseUrl(any(), any()) } returns Unit
        val viewModel = ApiConfigViewModel(apiConfigStore, apiProbe)
        viewModel.onUrlChange("http://192.168.0.10:3000")

        viewModel.guardar()

        coVerify(exactly = 1) { apiConfigStore.setApiBaseUrl("http://192.168.0.10:3000/", "1.4.0") }
        val state = viewModel.uiState.value
        assertTrue(state.guardadoExitoso)
        assertEquals("1.4.0", state.apiVersion)
        assertTrue(state.verificacionOk!!.contains("API v1.4.0"))
        assertFalse(state.isVerificando)
    }

    /** `GET /version` devuelve "dev" sin APP_VERSION inyectada: no debe leerse como "API vdev". */
    @Test
    fun `una version no numerica se muestra sin el prefijo v`() = runTest {
        givenStore(url = null)
        coEvery { apiProbe.probe(any()) } returns ApiProbeResult.Ok("dev")
        coEvery { apiConfigStore.setApiBaseUrl(any(), any()) } returns Unit
        val viewModel = ApiConfigViewModel(apiConfigStore, apiProbe)
        viewModel.onUrlChange("http://192.168.0.10:3000")

        viewModel.guardar()

        val mensaje = viewModel.uiState.value.verificacionOk!!
        assertTrue(mensaje.contains("API dev"))
        assertFalse(mensaje.contains("vdev"))
    }

    @Test
    fun `guardar con servidor inalcanzable no persiste y muestra error de conexion`() = runTest {
        givenStore(url = null)
        coEvery { apiProbe.probe(any()) } returns ApiProbeResult.Unreachable
        val viewModel = ApiConfigViewModel(apiConfigStore, apiProbe)
        viewModel.onUrlChange("http://192.168.0.99:3000")

        viewModel.guardar()

        coVerify(exactly = 0) { apiConfigStore.setApiBaseUrl(any(), any()) }
        val state = viewModel.uiState.value
        assertFalse(state.guardadoExitoso)
        assertFalse(state.isVerificando)
        assertTrue(state.errorMessage!!.contains("No se pudo conectar"))
    }

    @Test
    fun `guardar con base de datos caida no persiste y nombra la base que falla`() = runTest {
        givenStore(url = null)
        coEvery { apiProbe.probe(any()) } returns
            ApiProbeResult.DbUnavailable(postgresOk = true, mssqlOk = false)
        val viewModel = ApiConfigViewModel(apiConfigStore, apiProbe)
        viewModel.onUrlChange("http://192.168.0.10:3000")

        viewModel.guardar()

        coVerify(exactly = 0) { apiConfigStore.setApiBaseUrl(any(), any()) }
        val state = viewModel.uiState.value
        assertFalse(state.guardadoExitoso)
        assertTrue(state.errorMessage!!.contains("SQL Server"))
    }

    @Test
    fun `guardar contra algo que no es el API no persiste y avisa que no es valido`() = runTest {
        givenStore(url = null)
        coEvery { apiProbe.probe(any()) } returns ApiProbeResult.NotHealthy("el servidor respondió HTTP 404")
        val viewModel = ApiConfigViewModel(apiConfigStore, apiProbe)
        viewModel.onUrlChange("http://192.168.0.10:8080")

        viewModel.guardar()

        coVerify(exactly = 0) { apiConfigStore.setApiBaseUrl(any(), any()) }
        val state = viewModel.uiState.value
        assertFalse(state.guardadoExitoso)
        assertTrue(state.errorMessage!!.contains("no es un servidor de Expedición válido"))
    }

    @Test
    fun `editar la url descarta la verificacion anterior`() = runTest {
        givenStore(url = null)
        coEvery { apiProbe.probe(any()) } returns ApiProbeResult.Ok("1.4.0")
        coEvery { apiConfigStore.setApiBaseUrl(any(), any()) } returns Unit
        val viewModel = ApiConfigViewModel(apiConfigStore, apiProbe)
        viewModel.onUrlChange("http://192.168.0.10:3000")
        viewModel.guardar()
        assertNotNull(viewModel.uiState.value.verificacionOk)

        viewModel.onUrlChange("http://otro-servidor:3000")

        assertNull(viewModel.uiState.value.verificacionOk)
    }

    private fun givenStore(url: String?, version: String? = null) {
        every { apiConfigStore.apiBaseUrl } returns flowOf(url)
        every { apiConfigStore.apiVersion } returns flowOf(version)
    }
}
