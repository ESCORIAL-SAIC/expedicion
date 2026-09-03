package com.expedicion.app.ui.login

import com.expedicion.app.MainDispatcherRule
import com.expedicion.app.data.ApiResult
import com.expedicion.app.data.config.ApiConfigStore
import com.expedicion.app.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()
    private val apiConfigStore = mockk<ApiConfigStore>().also {
        // La version del API en Login es puramente informativa; ningun test de login la ejercita.
        every { it.apiVersion } returns flowOf(null)
    }

    /**
     * `UnitIngreso.pas`: `UsuarioValido('','')` devuelve False sin consultar la base, pero el
     * flujo igual limpia los campos y muestra el dialogo "Datos de acceso incorrectos..."
     * (UnitIngreso.pas:59-76). Se replica ambas partes: no se llama al backend, pero si se
     * muestra el mensaje.
     */
    @Test
    fun `ambos campos vacios no llama al repository pero muestra el mensaje de error`() = runTest {
        val viewModel = LoginViewModel(authRepository, apiConfigStore)

        viewModel.submit()

        coVerify(exactly = 0) { authRepository.login(any(), any()) }
        val state = viewModel.uiState.value
        assertEquals("Datos de acceso incorrectos. Vuelva a intentarlo.", state.errorMessage)
        assertEquals("", state.usuario)
        assertEquals("", state.password)
    }

    @Test
    fun `login exitoso limpia campos y marca loginSuccess`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns ApiResult.Success(Unit)
        val viewModel = LoginViewModel(authRepository, apiConfigStore)
        viewModel.onUsuarioChange("jperez")
        viewModel.onPasswordChange("1234")

        viewModel.submit()

        val state = viewModel.uiState.value
        assertEquals(true, state.loginSuccess)
        assertEquals("", state.usuario)
        assertEquals("", state.password)
        assertNull(state.errorMessage)
    }

    @Test
    fun `login invalido muestra el mensaje exacto del API y limpia campos`() = runTest {
        val mensaje = "Datos de acceso incorrectos. Vuelva a intentarlo."
        coEvery { authRepository.login(any(), any()) } returns ApiResult.Error(mensaje, "INVALID_CREDENTIALS", 401)
        val viewModel = LoginViewModel(authRepository, apiConfigStore)
        viewModel.onUsuarioChange("jperez")
        viewModel.onPasswordChange("mala")

        viewModel.submit()

        val state = viewModel.uiState.value
        assertEquals(mensaje, state.errorMessage)
        assertEquals(false, state.loginSuccess)
        assertEquals("", state.usuario)
        assertEquals("", state.password)
    }

    /**
     * `UnitIngreso.pas` `ButtonIngresarClick`: el bloque `if ModuloDatos.FDConnection.Connected`
     * es el UNICO que limpia `EditUsuario`/`EditPassword`; la rama `else` (sin conexion a la base,
     * equivalente a nuestro 503 DB_UNAVAILABLE) no toca los campos, preservando lo escrito para
     * reintentar. `isServerFault()` preserva los campos para httpStatus >= 500 (y para null, ver
     * el test de falla de red mas abajo).
     */
    @Test
    fun `login con error 503 no deberia limpiar los campos (preserva contexto para reintento)`() = runTest {
        val mensaje503 = "No se pudo establecer conexión con la Base de Datos. Cierre la aplicación, revise su configuración de red y vuelva a intentar abrir la aplicación."
        coEvery { authRepository.login(any(), any()) } returns ApiResult.Error(mensaje503, "DB_UNAVAILABLE", 503)
        val viewModel = LoginViewModel(authRepository, apiConfigStore)
        viewModel.onUsuarioChange("jperez")
        viewModel.onPasswordChange("1234")

        viewModel.submit()

        val state = viewModel.uiState.value
        assertEquals(mensaje503, state.errorMessage)
        assertEquals("jperez", state.usuario)
        assertEquals("1234", state.password)
    }

    /**
     * `UnitIngreso.pas`: la rama `else` de `ModuloDatos.FDConnection.Connected` (sin conexion)
     * no toca los campos. El equivalente Android es una falla pura de transporte (sin respuesta
     * HTTP: `httpStatus = null`), que debe comportarse igual que un 503 y preservar lo escrito.
     */
    @Test
    fun `falla de red sin respuesta del servidor no limpia los campos`() = runTest {
        val mensajeRed = "No se pudo conectar con el servidor. Verifique su conexión de red."
        coEvery { authRepository.login(any(), any()) } returns
            ApiResult.Error(mensajeRed, httpStatus = null)
        val viewModel = LoginViewModel(authRepository, apiConfigStore)
        viewModel.onUsuarioChange("jperez")
        viewModel.onPasswordChange("1234")

        viewModel.submit()

        val state = viewModel.uiState.value
        assertEquals(mensajeRed, state.errorMessage)
        assertEquals("jperez", state.usuario)
        assertEquals("1234", state.password)
    }
}
