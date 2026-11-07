package com.example.dam_t1_evaluacion_practica.ui.viewmodel

import com.example.dam_t1_evaluacion_practica.domain.model.IncidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.domain.model.User
import com.example.dam_t1_evaluacion_practica.domain.repository.InspectionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Design section 9: `restoreSession` mirrors the success branch of `login`, so a
 * restored session shows the same user and the same user-scoped inspections.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InspectionViewModelSessionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<InspectionRepository>(relaxed = true)
    private lateinit var viewModel: InspectionViewModel

    private val user = User(
        email = "test@seguridad.mx",
        name = "Ing. Test",
        cedula = "CED-1234",
        uid = "uid-1"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = InspectionViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun record(folio: String, email: String): InspectionSessionRecord = InspectionSessionRecord(
        folio = folio,
        inspectionId = "id-$folio",
        userEmail = email,
        companyName = "Ternium",
        zoneName = "Zona",
        auditType = "Ordinaria",
        inspectorName = user.name,
        inspectorCedula = user.cedula,
        timestamp = 1_700_000_000_000L,
        overallScore = 90,
        criticalStop = false,
        reinspectionDue = false,
        modules = emptyList(),
        findings = emptyList(),
        normCodes = emptyList(),
        status = IncidenceStatus.CLOSED
    )

    @Test
    fun `restoreSession sets the current user and observes the user inspections`() = runTest {
        val records = listOf(record("AUD-1", user.email))
        coEvery { repository.getInspectionsByUser(user.email) } returns flowOf(records)

        viewModel.restoreSession(user)
        advanceUntilIdle()

        assertEquals(user, viewModel.uiState.value.currentUser)
        assertEquals(user.name, viewModel.uiState.value.inspectorName)
        assertEquals(records, viewModel.uiState.value.savedInspections)
        assertNull(viewModel.uiState.value.authError)
    }

    @Test
    fun `restoreSession replaces the global inspection list with the user scope`() = runTest {
        val globalRecords = listOf(record("AUD-0", "otro@seguridad.mx"))
        val userRecords = listOf(record("AUD-1", user.email))
        coEvery { repository.getAllInspections() } returns flowOf(globalRecords)
        coEvery { repository.getInspectionsByUser(user.email) } returns flowOf(userRecords)

        // Built after the stubs: the ViewModel starts observing in its constructor.
        val scoped = InspectionViewModel(repository)
        advanceUntilIdle()
        assertEquals(globalRecords, scoped.uiState.value.savedInspections)

        scoped.restoreSession(user)
        advanceUntilIdle()

        assertEquals(userRecords, scoped.uiState.value.savedInspections)
    }

    @Test
    fun `restoreSession mirrors the login success branch`() = runTest {
        val records = listOf(record("AUD-1", user.email))
        coEvery { repository.login(user.email, "pass") } returns user
        coEvery { repository.getInspectionsByUser(user.email) } returns flowOf(records)

        viewModel.login(user.email, "pass")
        advanceUntilIdle()
        val afterLogin = viewModel.uiState.value

        val restored = InspectionViewModel(repository)
        restored.restoreSession(user)
        advanceUntilIdle()
        val afterRestore = restored.uiState.value

        assertEquals(afterLogin.currentUser, afterRestore.currentUser)
        assertEquals(afterLogin.inspectorName, afterRestore.inspectorName)
        assertEquals(afterLogin.savedInspections, afterRestore.savedInspections)
        assertEquals(afterLogin.authError, afterRestore.authError)
    }
}
