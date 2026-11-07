package com.example.dam_t1_evaluacion_practica.ui.viewmodel

import com.example.dam_t1_evaluacion_practica.domain.error.AuthError
import com.example.dam_t1_evaluacion_practica.domain.error.AuthException
import com.example.dam_t1_evaluacion_practica.domain.repository.InspectionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Design section 9: the ViewModel maps an [AuthException] to the existing Spanish copy.
 * Raw provider codes never reach the UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InspectionViewModelAuthTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<InspectionRepository>(relaxed = true)
    private lateinit var viewModel: InspectionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = InspectionViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login maps invalid credentials to the existing Spanish message`() = runTest {
        coEvery { repository.login(any(), any()) } throws AuthException(AuthError.INVALID_CREDENTIALS)

        var reported: String? = null
        viewModel.login("test@seguridad.mx", "pass", onError = { reported = it })
        advanceUntilIdle()

        assertEquals("Credenciales incorrectas. Verifica tu correo y contraseña.", reported)
        assertEquals("Credenciales incorrectas. Verifica tu correo y contraseña.", viewModel.uiState.value.authError)
        assertNull(viewModel.uiState.value.currentUser)
        assertFalse(viewModel.uiState.value.isLoadingAuth)
    }

    @Test
    fun `register maps a duplicate email to the existing Spanish message`() = runTest {
        coEvery { repository.getUserByEmail(any()) } returns null
        coEvery { repository.registerUser(any(), any()) } throws AuthException(AuthError.EMAIL_ALREADY_IN_USE)

        var reported: String? = null
        viewModel.register(
            name = "Ing. Nuevo",
            email = "nuevo@seguridad.mx",
            cedula = "STPS-1",
            password = "password123",
            onError = { reported = it }
        )
        advanceUntilIdle()

        assertEquals("Este correo ya está registrado", reported)
        assertNull(viewModel.uiState.value.currentUser)
    }

    @Test
    fun `login reports an offline failure in Spanish without the provider code`() = runTest {
        coEvery { repository.login(any(), any()) } throws AuthException(AuthError.NETWORK_UNAVAILABLE)

        viewModel.login("test@seguridad.mx", "pass")
        advanceUntilIdle()

        assertEquals("Sin conexión a internet. Revisa tu red e inténtalo de nuevo.", viewModel.uiState.value.authError)
    }

    @Test
    fun `no auth error ever leaks the provider code to the UI`() = runTest {
        for (error in AuthError.entries) {
            coEvery { repository.login(any(), any()) } throws AuthException(error)

            viewModel.login("test@seguridad.mx", "pass")
            advanceUntilIdle()

            val message = viewModel.uiState.value.authError
            assertNotNull("no message for $error", message)
            assertFalse("leaked ${error.name} in $message", message!!.contains(error.name))
            assertFalse("leaked provider code in $message", message.contains("ERROR_"))
        }
    }
}
