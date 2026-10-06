package com.example.dam_t1_evaluacion_practica.ui.viewmodel

import com.example.dam_t1_evaluacion_practica.data.repository.InspectionRepository
import com.example.dam_t1_evaluacion_practica.domain.model.ChecklistItem
import com.example.dam_t1_evaluacion_practica.domain.model.CriticalityLevel
import com.example.dam_t1_evaluacion_practica.domain.model.IncidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.model.Normativ
import com.example.dam_t1_evaluacion_practica.domain.model.User
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InspectionViewModelTest {

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

    private fun item(id: String, level: CriticalityLevel): ChecklistItem = ChecklistItem(
        id = id,
        description = "Item $id",
        normativ = Normativ(code = "NOM-TEST", description = "Norma de prueba"),
        criticalityLevel = level,
        requiresPhotoEvidence = false
    )

    /** Total weight 13 = HIGH(5) + MEDIUM(3) + MEDIUM(3) + LOW(1) + LOW(1). */
    private fun checklistTotalWeight13(): List<ChecklistItem> = listOf(
        item("w5", CriticalityLevel.HIGH),
        item("w3a", CriticalityLevel.MEDIUM),
        item("w3b", CriticalityLevel.MEDIUM),
        item("w1a", CriticalityLevel.LOW),
        item("w1b", CriticalityLevel.LOW)
    )

    /** Seven HIGH items, total weight 35. */
    private fun checklistSevenHigh(): List<ChecklistItem> =
        (1..7).map { item("high_$it", CriticalityLevel.HIGH) }

    // --- calculateScore: weighted floor score ---

    @Test
    fun `calculateScore floors weighted result -- 9 of 13 returns 69`() {
        val items = checklistTotalWeight13()
        val answers = mapOf("w5" to true, "w3a" to true, "w1a" to true) // obtained = 9

        val score = viewModel.calculateScore(items, answers)

        assertEquals(69, score) // floor(9 * 100 / 13) == 69, never rounded to 69.2
    }

    @Test
    fun `calculateScore counts only true answers -- false and absent contribute zero`() {
        // HIGH(5) + MEDIUM(3) + LOW(1) => total 9; only HIGH answered true => obtained 5.
        val items = listOf(
            item("h", CriticalityLevel.HIGH),
            item("m", CriticalityLevel.MEDIUM),
            item("l", CriticalityLevel.LOW)
        )
        val answers = mapOf("h" to true, "m" to false) // "l" absent from answers

        val score = viewModel.calculateScore(items, answers)

        assertEquals(55, score) // floor(5 * 100 / 9) == 55
    }

    @Test
    fun `calculateScore all compliant returns 100`() {
        val items = checklistTotalWeight13()
        val answers = items.associate { it.id to true }

        val score = viewModel.calculateScore(items, answers)

        assertEquals(100, score)
    }

    @Test
    fun `calculateScore no answers returns 0`() {
        val score = viewModel.calculateScore(checklistTotalWeight13(), emptyMap())

        assertEquals(0, score)
    }

    @Test
    fun `calculateScore empty checklist returns 100`() {
        val score = viewModel.calculateScore(emptyList(), emptyMap())

        assertEquals(100, score)
    }

    // --- isReinspectionDue: strict < 85 threshold ---

    @Test
    fun `isReinspectionDue score 84 returns true`() {
        assertTrue(viewModel.isReinspectionDue(84))
    }

    @Test
    fun `isReinspectionDue score 85 returns false`() {
        assertFalse(viewModel.isReinspectionDue(85))
    }

    // --- scheduleReinspection: 24h window ---

    @Test
    fun `scheduleReinspection adds exactly 24h to now`() {
        val nowMillis = 1_700_000_000_000L

        val scheduled = viewModel.scheduleReinspection(nowMillis)

        assertEquals(nowMillis + REINSPECTION_WINDOW_MS, scheduled)
    }

    @Test
    fun `scheduleReinspection is deterministic for a fixed now`() {
        val nowMillis = 1_700_000_000_000L

        val first = viewModel.scheduleReinspection(nowMillis)
        val second = viewModel.scheduleReinspection(nowMillis)

        assertEquals(first, second)
    }

    // --- submitAnswer: score rises live; critical stop fires LIVE only on HIGH non-compliant ---

    @Test
    fun `submitAnswer compliant answers never fire live critical stop -- 7 HIGH 6 answered`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        items.take(6).forEach { viewModel.submitAnswer(it, isCompliant = true) } // one HIGH unanswered
        viewModel.submitAnswer(items[6], isCompliant = true) // all 7 compliant => score 100

        val state = viewModel.uiState.value
        assertEquals(100, state.overallComplianceScore)
        assertFalse(state.criticalStopTriggered) // compliant answers never fire the live stop
        assertFalse(state.reinspectionDue)       // reinspection stays clear until finish
    }

    @Test
    fun `submitAnswer below-threshold score keeps reinspection false until finish but fires live stop on HIGH non-compliant`() {
        // Total 33 = 6 HIGH (30) + 1 MEDIUM (3); answer 5 HIGH + MEDIUM => obtained 28 => 84.
        val items = (1..6).map { item("high_$it", CriticalityLevel.HIGH) } +
            item("med", CriticalityLevel.MEDIUM)
        viewModel.initChecklist(items)

        items.filter { it.id != "high_6" }.forEach { viewModel.submitAnswer(it, isCompliant = true) }

        var state = viewModel.uiState.value
        assertEquals(84, state.overallComplianceScore) // floor(28 * 100 / 33) == 84
        assertFalse(state.reinspectionDue)             // reinspection is only evaluated at finish

        // Marking the remaining HIGH item non-compliant fires the critical stop LIVE...
        viewModel.submitAnswer(items.first { it.id == "high_6" }, isCompliant = false)

        state = viewModel.uiState.value
        assertEquals(84, state.overallComplianceScore)
        assertTrue(state.criticalStopTriggered) // HIGH non-compliant fires immediately, live
        assertFalse(state.reinspectionDue)      // ...but reinspection still waits for finish

        viewModel.finishModule("Modulo EPP", items)
        assertTrue(viewModel.uiState.value.reinspectionDue) // score 84 < 85 => reinspection at finish
    }

    @Test
    fun `HIGH non-compliant answer triggers critical stop live even with a high score`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        // 6 of 7 HIGH compliant => score 85, at the threshold; the 7th HIGH answered
        // non-compliant must still fire the critical stop LIVE (score-independent rule).
        items.take(6).forEach { viewModel.submitAnswer(it, isCompliant = true) }
        viewModel.submitAnswer(items[6], isCompliant = false)

        val state = viewModel.uiState.value
        assertEquals(85, state.overallComplianceScore) // floor(30 * 100 / 35) == 85
        assertTrue(state.criticalStopTriggered)        // live fire, regardless of the score
        assertFalse(state.reinspectionDue)             // reinspection still evaluated at finish
    }

    @Test
    fun `MEDIUM or LOW non-compliant answers do not fire the live critical stop`() {
        val items = checklistTotalWeight13()
        viewModel.initChecklist(items)

        viewModel.submitAnswer(items[1], isCompliant = false) // MEDIUM non-compliant
        viewModel.submitAnswer(items[3], isCompliant = false) // LOW non-compliant
        viewModel.submitAnswer(items[4], isCompliant = false) // LOW non-compliant

        val state = viewModel.uiState.value
        assertEquals(0, state.overallComplianceScore)
        assertFalse(state.criticalStopTriggered)    // non-HIGH findings never fire live
        assertFalse(state.reinspectionDue)
        assertFalse(state.criticalEvidencePending)  // only HIGH findings demand evidence
    }

    // --- criticalEvidencePending: HIGH findings without photo or note ---

    @Test
    fun `criticalEvidencePending true while a HIGH finding lacks photo or note, false once attached`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        viewModel.submitAnswer(items.first(), isCompliant = false, photoUri = null, notes = null)

        assertTrue(viewModel.uiState.value.criticalEvidencePending) // lacks photo AND note

        // Attaching only a photo keeps it pending: the note is still missing.
        viewModel.attachFindingEvidence(items.first().id, "content://gallery/foto", null, null)
        assertTrue(viewModel.uiState.value.criticalEvidencePending)

        // Attaching the note closes the pending state (photo was already saved).
        viewModel.attachFindingEvidence(items.first().id, null, null, null, notes = "Fisura detectada en el arnés")
        assertFalse(viewModel.uiState.value.criticalEvidencePending)
    }

    @Test
    fun `compliant answer on a pending HIGH finding clears criticalEvidencePending`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        viewModel.submitAnswer(items.first(), isCompliant = false, photoUri = null, notes = null)
        assertTrue(viewModel.uiState.value.criticalEvidencePending)

        viewModel.submitAnswer(items.first(), isCompliant = true) // inspector corrects the answer

        assertFalse(viewModel.uiState.value.criticalEvidencePending)
        assertTrue(viewModel.uiState.value.criticalStopTriggered) // flag stays true once fired
    }

    // --- criticalPendingItemIds: derived set of item ids still demanding evidence ---

    @Test
    fun `HIGH non-compliant without evidence populates criticalPendingItemIds and compliant correction on a still-pending finding keeps the set non-empty`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        viewModel.submitAnswer(items.first(), isCompliant = false, photoUri = null, notes = null)

        assertTrue(viewModel.uiState.value.criticalPendingItemIds.contains(items.first().id))

        viewModel.attachFindingEvidence(items.first().id, "content://gallery/foto", null, null, notes = "Arnés desgastado")

        assertTrue(viewModel.uiState.value.criticalPendingItemIds.isEmpty())
        assertFalse(viewModel.uiState.value.criticalEvidencePending)
    }

    @Test
    fun `NA answers never enter criticalPendingItemIds`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        viewModel.setAnswerNA(items.first())

        assertFalse(viewModel.uiState.value.criticalPendingItemIds.contains(items.first().id))
        assertFalse(viewModel.uiState.value.criticalEvidencePending)
    }

    @Test
    fun `NA answer on a HIGH item does not fire the LIVE critical stop but keeps counting for the score`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        viewModel.setAnswerNA(items.first())

        val state = viewModel.uiState.value
        // N/A is not a real non-conformance: it must never open the evidence dialog...
        assertFalse(state.criticalStopTriggered)
        // ...but it stores false, so finishing below 85 still schedules a reinspection.
        viewModel.finishModule("Modulo EPP", items)
        val finished = viewModel.uiState.value
        assertTrue(finished.sessionModules.any { it.moduleTitle == "Modulo EPP" && it.reinspectionDue })
        assertTrue(finished.criticalStopTriggered) // score-based stop on finish, not a live N/A fire
    }

    @Test
    fun `attachFindingEvidence with empty-string notes leaves evidence pending`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        viewModel.submitAnswer(items.first(), isCompliant = false, photoUri = "content://gallery/foto", notes = null)
        viewModel.attachFindingEvidence(items.first().id, null, null, null, notes = "")

        assertTrue(viewModel.uiState.value.criticalPendingItemIds.contains(items.first().id))
        assertTrue(viewModel.uiState.value.criticalEvidencePending)
    }

    // --- criticalNonCompliantItemIds: HIGH non-compliant findings lock forever ---

    @Test
    fun `criticalNonCompliantItemIds keeps a HIGH finding even after evidence is attached`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        val first = items.first()
        viewModel.submitAnswer(first, isCompliant = false, photoUri = null, notes = null)
        viewModel.attachFindingEvidence(first.id, "content://gallery/foto", null, null, notes = "Arnés desgastado")

        val state = viewModel.uiState.value
        // The evidence gate is closed (photo + note present)...
        assertFalse(state.criticalEvidencePending)
        // ...but the item stays permanently locked as a HIGH non-compliant finding.
        assertTrue(state.criticalNonCompliantItemIds.contains(first.id))
    }

    @Test
    fun `criticalNonCompliantItemIds ignores NA findings`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        viewModel.setAnswerNA(items.first())

        // An N/A answer is not a real non-conformance: it must never enter the permanent
        // lock set, so the inspector can still change the tri-state later.
        assertFalse(viewModel.uiState.value.criticalNonCompliantItemIds.contains(items.first().id))
    }

    // --- saveDictamen: scheduled reinspection date (24h) ---

    @Test
    fun `saveDictamen schedules reinspection exactly 24h ahead when reinspectionDue`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        items.take(5).forEach { viewModel.submitAnswer(it, isCompliant = true) } // 25/35 = 71
        viewModel.finishModule("Modulo EPP", items)
        assertTrue(viewModel.uiState.value.reinspectionDue)

        val before = System.currentTimeMillis()
        val record = viewModel.saveDictamen()
        val after = System.currentTimeMillis()

        assertTrue(record.reinspectionDue)
        val scheduled = record.scheduledReinspectionAt
        assertNotNull(scheduled)
        assertTrue(scheduled!! >= before + REINSPECTION_WINDOW_MS)
        assertTrue(scheduled <= after + REINSPECTION_WINDOW_MS)
    }

    @Test
    fun `saveDictamen leaves scheduledReinspectionAt null when no reinspection is due`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        items.forEach { viewModel.submitAnswer(it, isCompliant = true) } // 100
        viewModel.finishModule("Modulo EPP", items)

        val record = viewModel.saveDictamen()

        assertFalse(record.reinspectionDue)
        assertNull(record.scheduledReinspectionAt)
    }

    // --- score progression ---

    @Test
    fun `submitAnswer score starts at 0 and rises with each compliant answer`() {
        val items = listOf(
            item("h", CriticalityLevel.HIGH),
            item("m", CriticalityLevel.MEDIUM),
            item("l", CriticalityLevel.LOW)
        )
        viewModel.initChecklist(items)

        // A loaded, non-empty checklist starts at 0 (never 100).
        assertEquals(0, viewModel.uiState.value.overallComplianceScore)

        viewModel.submitAnswer(items[0], isCompliant = true) // HIGH => obtained 5 of 9

        val state = viewModel.uiState.value
        assertEquals(55, state.overallComplianceScore) // floor(5 * 100 / 9) == 55
        assertFalse(state.criticalStopTriggered)
    }

    // --- finishModule: the ONLY place where stop flags are evaluated ---

    @Test
    fun `finishModule below 85 triggers critical stop and reinspection`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        items.take(5).forEach { viewModel.submitAnswer(it, isCompliant = true) } // 25/35 = 71

        viewModel.finishModule("Modulo EPP", items)

        val state = viewModel.uiState.value
        assertEquals(71, state.overallComplianceScore) // floor(25 * 100 / 35) == 71
        assertTrue(state.criticalStopTriggered)
        assertTrue(state.reinspectionDue)

        val snapshot = state.sessionModules.first { it.moduleTitle == "Modulo EPP" }
        assertTrue(snapshot.completed)
        assertTrue(snapshot.criticalStop)
        assertTrue(snapshot.reinspectionDue)
    }

    @Test
    fun `finishModule at least 85 does not trigger critical stop`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        items.take(6).forEach { viewModel.submitAnswer(it, isCompliant = true) } // 30/35 = 85

        viewModel.finishModule("Modulo Maquinaria", items)

        val state = viewModel.uiState.value
        assertEquals(85, state.overallComplianceScore) // floor(30 * 100 / 35) == 85
        assertFalse(state.criticalStopTriggered)
        assertFalse(state.reinspectionDue)

        val snapshot = state.sessionModules.first { it.moduleTitle == "Modulo Maquinaria" }
        assertFalse(snapshot.criticalStop)
        assertFalse(snapshot.reinspectionDue)
    }

    @Test
    fun `finishModule flags stay clear while answering partially`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        items.take(3).forEach { viewModel.submitAnswer(it, isCompliant = true) }

        val state = viewModel.uiState.value
        assertEquals(42, state.overallComplianceScore) // floor(15 * 100 / 35) == 42
        assertFalse(state.criticalStopTriggered)
        assertFalse(state.reinspectionDue)
    }

    // --- evidence with real location ---

    @Test
    fun `attachFindingEvidence updates finding photo and coordinates`() {
        val items = checklistTotalWeight13()
        viewModel.initChecklist(items)

        viewModel.submitAnswer(items.first(), isCompliant = false, photoUri = null, notes = "Hallazgo sin foto")

        viewModel.attachFindingEvidence(items.first().id, "content://gallery/evidencia", 19.4326, -99.1332)

        val state = viewModel.uiState.value
        val finding = state.sessionFindings.first { it.itemId == items.first().id }
        assertEquals("content://gallery/evidencia", finding.photoUri)
        assertEquals(19.4326, finding.latitude)
        assertEquals(-99.1332, finding.longitude)
    }

    @Test
    fun `saveDictamen uses real captured location when a finding has GPS coordinates`() {
        val items = checklistTotalWeight13()
        viewModel.initChecklist(items)

        viewModel.submitAnswer(
            items.first(), isCompliant = false,
            photoUri = "content://gallery/foto", notes = "Sin casco",
            lat = 25.6866, lng = -100.3161
        )

        val record = viewModel.saveDictamen()

        assertEquals(25.6866, record.latitude)
        assertEquals(-100.3161, record.longitude)
    }

    @Test
    fun `saveDictamen falls back to site position when no finding has GPS coordinates`() {
        val items = checklistTotalWeight13()
        viewModel.initChecklist(items)

        viewModel.submitAnswer(items.first(), isCompliant = false, photoUri = null, notes = "Sin GPS")
        viewModel.submitAnswer(items[1], isCompliant = false, photoUri = null, notes = "Sin GPS")

        val record = viewModel.saveDictamen()

        assertEquals(25.6866, record.latitude)  // DEFAULT_SITE_LATITUDE
        assertEquals(-100.3161, record.longitude) // DEFAULT_SITE_LONGITUDE
    }

    // --- initChecklist: full reset of sticky state ---

    @Test
    fun `initChecklist resets sticky state -- answers, stop, score, reinspectionDue`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)
        viewModel.submitAnswer(items.first(), isCompliant = true) // leaves answers, stop and a stored score

        viewModel.initChecklist(items) // a new inspection starts

        val state = viewModel.uiState.value
        assertTrue(state.answers.isEmpty())
        assertFalse(state.criticalStopTriggered)
        assertEquals(0, state.overallComplianceScore) // fresh checklist starts at 0, not 100
        assertFalse(state.reinspectionDue)
    }

    // --- restored modules: score recomputed, stop flags stay clear ---

    @Test
    fun `initChecklist restored module recomputes score without stop flags`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)
        items.take(5).forEach { viewModel.submitAnswer(it, isCompliant = true) } // 25/35 = 71
        viewModel.finishModule("Modulo EPP", items)
        assertTrue(viewModel.uiState.value.criticalStopTriggered)

        // Re-opening the finished module restores answers and recomputes the score,
        // but never re-triggers the live dialog.
        viewModel.initChecklist(items, items.associate { it.id to true })

        val state = viewModel.uiState.value
        assertEquals(100, state.overallComplianceScore)
        assertFalse(state.criticalStopTriggered)
        assertFalse(state.reinspectionDue)
    }

    // --- submitAnswer: re-submission preserves evidence attached via the dialog ---

    @Test
    fun `submitAnswer preserves photo evidence when re-submitted without new photo`() {
        val items = checklistTotalWeight13()
        viewModel.initChecklist(items)
        val target = items.first() // HIGH

        // First submission carries photo + GPS evidence.
        viewModel.submitAnswer(
            target, isCompliant = false,
            photoUri = "content://test", notes = "obs",
            lat = 25.6866, lng = -100.3161
        )

        // Re-submission (e.g. a keystroke in the note field) sends photoUri = null and no
        // coordinates: the existing photo and location must be preserved, not dropped.
        viewModel.submitAnswer(
            target, isCompliant = false,
            photoUri = null, notes = "obs2", lat = null, lng = null
        )

        val findings = viewModel.uiState.value.sessionFindings.filter { it.itemId == target.id }
        assertEquals(1, findings.size)
        val finding = findings.single()
        assertEquals("content://test", finding.photoUri)
        assertEquals("obs2", finding.notes)
        assertEquals(25.6866, finding.latitude)
    }

    @Test
    fun `submitAnswer preserves evidence attached via dialog when editing note`() {
        val items = checklistTotalWeight13()
        viewModel.initChecklist(items)
        val target = items.first() // HIGH

        // Non-compliant without evidence, then the Critical Stop dialog attaches photo + GPS + note.
        viewModel.submitAnswer(target, isCompliant = false)
        viewModel.attachFindingEvidence(target.id, "content://dialog", 25.5, -100.3, "obs")

        // Editing the note from the row re-submits with photoUri = null and no coordinates.
        viewModel.submitAnswer(target, isCompliant = false, photoUri = null, notes = "edited")

        val findings = viewModel.uiState.value.sessionFindings.filter { it.itemId == target.id }
        assertEquals(1, findings.size)
        val finding = findings.single()
        assertEquals("content://dialog", finding.photoUri)
        assertEquals("edited", finding.notes)
        assertEquals(25.5, finding.latitude)
    }

    // --- saveDictamen: closing status (IncidenceStatus) ---

    @Test
    fun `saveDictamen conforming record starts CLOSED and updateRecordStatus transitions it`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        items.forEach { viewModel.submitAnswer(it, isCompliant = true) } // 100, conforming
        viewModel.finishModule("Modulo EPP", items)

        val record = viewModel.saveDictamen()

        // A conforming dictamen starts CLOSED (neither criticalStop nor reinspectionDue).
        assertEquals(IncidenceStatus.CLOSED, record.status)
        assertEquals(IncidenceStatus.CLOSED, viewModel.uiState.value.savedInspections.single().status)

        // The inspector transitions the closing status (e.g. an opened mitigation).
        viewModel.updateRecordStatus(record.folio, IncidenceStatus.IN_MITIGATION)

        val updated = viewModel.uiState.value.savedInspections.single()
        assertEquals(IncidenceStatus.IN_MITIGATION, updated.status)
    }

    @Test
    fun `saveDictamen critical record starts OPEN`() {
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)

        items.take(5).forEach { viewModel.submitAnswer(it, isCompliant = true) } // 25/35 = 71
        viewModel.finishModule("Modulo EPP", items) // triggers critical stop + reinspection

        val record = viewModel.saveDictamen()

        // A record with critical stop or reinspection due stays OPEN until transitioned.
        assertTrue(record.criticalStop)
        assertTrue(record.reinspectionDue)
        assertEquals(IncidenceStatus.OPEN, record.status)
    }

    // --- User Authentication & Per-User Isolation Tests ---

    @Test
    fun `login with valid credentials sets currentUser and loads user inspections`() = runTest {
        val testUser = User(email = "test@seguridad.mx", name = "Ing. Test", cedula = "CED-1234")
        coEvery { repository.login("test@seguridad.mx", "secret123") } returns testUser
        coEvery { repository.getInspectionsByUser("test@seguridad.mx") } returns flowOf(emptyList())

        var successCalled = false
        viewModel.login(
            email = "test@seguridad.mx",
            password = "secret123",
            onSuccess = { successCalled = true }
        )
        advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(testUser, viewModel.uiState.value.currentUser)
        assertEquals("Ing. Test", viewModel.uiState.value.inspectorName)
        assertNull(viewModel.uiState.value.authError)
    }

    @Test
    fun `login with invalid credentials sets error`() = runTest {
        coEvery { repository.login("wrong@seguridad.mx", "badpass") } returns null

        var errorCalled = false
        viewModel.login(
            email = "wrong@seguridad.mx",
            password = "badpass",
            onError = { errorCalled = true }
        )
        advanceUntilIdle()

        assertTrue(errorCalled)
        assertNull(viewModel.uiState.value.currentUser)
        assertNotNull(viewModel.uiState.value.authError)
    }

    @Test
    fun `register new user persists account and isolates inspection scope`() = runTest {
        coEvery { repository.getUserByEmail("newuser@seguridad.mx") } returns null
        coEvery { repository.registerUser(any(), any()) } returns 1L
        coEvery { repository.getInspectionsByUser("newuser@seguridad.mx") } returns flowOf(emptyList())

        var registered = false
        viewModel.register(
            name = "Ing. Nuevo Inspector",
            email = "newuser@seguridad.mx",
            cedula = "STPS-9999",
            password = "password123",
            onSuccess = { registered = true }
        )
        advanceUntilIdle()

        assertTrue(registered)
        val user = viewModel.uiState.value.currentUser
        assertNotNull(user)
        assertEquals("newuser@seguridad.mx", user?.email)
        assertEquals("Ing. Nuevo Inspector", user?.name)

        // Creating inspection with this user tags it with user's email
        val items = checklistSevenHigh()
        viewModel.initChecklist(items)
        items.forEach { viewModel.submitAnswer(it, isCompliant = true) }
        viewModel.finishModule("Modulo EPP", items)

        val record = viewModel.saveDictamen()
        assertEquals("newuser@seguridad.mx", record.userEmail)
        assertEquals("Ing. Nuevo Inspector", record.inspectorName)
    }

    @Test
    fun `register duplicate email is rejected`() = runTest {
        val existing = User(email = "existing@seguridad.mx", name = "Existing", cedula = "CED-000")
        coEvery { repository.getUserByEmail("existing@seguridad.mx") } returns existing

        var errorReported = false
        viewModel.register(
            name = "Another",
            email = "existing@seguridad.mx",
            cedula = "CED-111",
            password = "password",
            onError = { errorReported = true }
        )
        advanceUntilIdle()

        assertTrue(errorReported)
        assertNull(viewModel.uiState.value.currentUser)
    }

    @Test
    fun `logout clears session and resets inspections`() = runTest {
        val user = User(email = "test@seguridad.mx", name = "Test", cedula = "CED-1")
        coEvery { repository.login(any(), any()) } returns user
        coEvery { repository.getInspectionsByUser(any()) } returns flowOf(emptyList())

        viewModel.login("test@seguridad.mx", "pass")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.currentUser)

        var loggedOut = false
        viewModel.logout { loggedOut = true }

        assertTrue(loggedOut)
        assertNull(viewModel.uiState.value.currentUser)
        assertTrue(viewModel.uiState.value.savedInspections.isEmpty())
    }
}