package com.expedicion.app.ui.navigation

import com.expedicion.app.MainDispatcherRule
import com.expedicion.app.data.config.ApiConfigStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppStartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiConfigStore = mockk<ApiConfigStore>()

    @Test
    fun `sin url guardada el destino inicial es ApiConfig`() = runTest {
        every { apiConfigStore.apiBaseUrl } returns flowOf(null)

        val viewModel = AppStartViewModel(apiConfigStore)

        assertEquals(Routes.API_CONFIG, viewModel.startDestination.value)
    }

    @Test
    fun `con url ya guardada -simulando un reinicio- el destino inicial es Login directo`() = runTest {
        every { apiConfigStore.apiBaseUrl } returns flowOf("https://servidor-actual.com/")

        // Nueva instancia del ViewModel, como ocurriría en un arranque nuevo del proceso.
        val viewModel = AppStartViewModel(apiConfigStore)

        assertEquals(Routes.LOGIN, viewModel.startDestination.value)
    }

    @Test
    fun `mientras no se resolvio la lectura del store el destino es null (loading)`() = runTest {
        // apiBaseUrl que nunca emite: simula la lectura en curso del DataStore antes del primer valor.
        every { apiConfigStore.apiBaseUrl } returns kotlinx.coroutines.flow.MutableSharedFlow<String?>()

        val viewModel = AppStartViewModel(apiConfigStore)

        assertEquals(null, viewModel.startDestination.value)
    }
}
