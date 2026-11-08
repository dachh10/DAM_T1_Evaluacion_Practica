package com.example.dam_t1_evaluacion_practica.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_t1_evaluacion_practica.domain.error.AuthError
import com.example.dam_t1_evaluacion_practica.domain.error.AuthException
import com.example.dam_t1_evaluacion_practica.domain.repository.InspectionRepository
import com.example.dam_t1_evaluacion_practica.domain.model.ChecklistItem
import com.example.dam_t1_evaluacion_practica.domain.model.CriticalityLevel
import com.example.dam_t1_evaluacion_practica.domain.model.Incidence
import com.example.dam_t1_evaluacion_practica.domain.model.IncidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.domain.model.ModuleCompletion
import com.example.dam_t1_evaluacion_practica.domain.model.SessionFinding
import com.example.dam_t1_evaluacion_practica.domain.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

internal const val REINSPECTION_THRESHOLD = 85
internal const val REINSPECTION_WINDOW_MS = 86_400_000L

/** Fixed inspector identity fallback used when no user is logged in. */
object InspectorProfile {
    const val NAME = "Ing. Diego Adrian"
    const val INITIALS = "DA"
    const val CEDULA = "STPS-84592"
    const val LICENSE = "Auditor Nivel 3"
    const val DC5 = "DC-5"
    const val CERT_CODE = "EC0391.01"
}

data class InspectionUiState(
    val currentChecklist: List<ChecklistItem> = emptyList(),
    val answers: Map<String, Boolean> = emptyMap(),
    val overallComplianceScore: Int = 0,
    val criticalStopTriggered: Boolean = false,
    val reinspectionDue: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    // --- Auth state ---
    val currentUser: User? = null,
    val authError: String? = null,
    val isLoadingAuth: Boolean = false,
    // --- Session context (Stitch redesign) ---
    val companyName: String = "Ternium Planta Monterrey Norte",
    val zoneName: String = "Área de Maquinaria y Calderas",
    val auditType: String = "Ordinaria",
    val inspectorName: String = InspectorProfile.NAME,
    // --- Session state ---
    val naAnswers: Set<String> = emptySet(),
    val sessionModules: List<ModuleCompletion> = emptyList(),
    val sessionFindings: List<SessionFinding> = emptyList(),
    val savedInspections: List<InspectionSessionRecord> = emptyList()
) {
    /**
     * Item ids of the session findings that are HIGH, non-N/A and still lack mandatory
     * evidence (photo OR note). The UI locks those rows and disables "Finalizar Módulo"
     * while any id remains pending so a critical finding can never be saved without its
     * evidence.
     */
    val criticalPendingItemIds: Set<String>
        get() = sessionFindings.filter { finding ->
            finding.criticalityLevel == CriticalityLevel.HIGH &&
                !finding.isNA &&
                (finding.photoUri.isNullOrBlank() || finding.notes.isNullOrBlank())
        }.map { it.itemId }.toSet()

    /**
     * True when at least one session finding is HIGH, non-N/A and still lacks mandatory
     * evidence (photo OR note). The UI disables "Finalizar Módulo" while this holds so a
     * critical finding can never be saved without its evidence.
     */
    val criticalEvidencePending: Boolean
        get() = criticalPendingItemIds.isNotEmpty()

    /**
     * Item ids of EVERY session finding that is HIGH and non-N/A, whether or not its
     * evidence was already attached. Once a HIGH item is documented as non-compliant it
     * stays locked FOREVER: the row can never be flipped back to "Conforme" or "N/A"
     * (the inspector may only refine the note/photo). This is the permanent lock,
     * independent of the mandatory-evidence gate ([criticalPendingItemIds]).
     */
    val criticalNonCompliantItemIds: Set<String>
        get() = sessionFindings.filter { finding ->
            finding.criticalityLevel == CriticalityLevel.HIGH && !finding.isNA
        }.map { it.itemId }.toSet()
}

class InspectionViewModel(
    private val repository: InspectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionUiState())
    val uiState: StateFlow<InspectionUiState> = _uiState.asStateFlow()


    private val persistedIncidenceIds = mutableMapOf<String, String>()

    private var activeInspectionId: String = UUID.randomUUID().toString()

    private var inspectionsJob: Job? = null

    init {

        observeAllInspections()
    }

    private fun observeAllInspections() {
        inspectionsJob?.cancel()
        inspectionsJob = viewModelScope.launch {
            repository.getAllInspections().collect { inspections ->
                // Only update if no specific user is logged in
                if (_uiState.value.currentUser == null) {
                    _uiState.update { it.copy(savedInspections = inspections) }
                }
            }
        }
    }

    private fun observeUserInspections(userEmail: String) {
        inspectionsJob?.cancel()
        inspectionsJob = viewModelScope.launch {
            repository.getInspectionsByUser(userEmail).collect { inspections ->
                _uiState.update { it.copy(savedInspections = inspections) }
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        if (trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
            val msg = "Ingresa tu correo y contraseña"
            _uiState.update { it.copy(authError = msg) }
            onError(msg)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAuth = true, authError = null) }
            try {
                val user = repository.login(trimmedEmail, trimmedPassword)
                if (user != null) {
                    applyAuthenticatedUser(user)
                    onSuccess()
                } else {
                    val msg = "Credenciales incorrectas. Verifica tu correo y contraseña."
                    _uiState.update { it.copy(isLoadingAuth = false, authError = msg) }
                    onError(msg)
                }
            } catch (auth: AuthException) {
                val msg = authErrorMessage(auth.error, "Error al iniciar sesión")
                _uiState.update { it.copy(isLoadingAuth = false, authError = msg) }
                onError(msg)
            } catch (e: Exception) {
                val msg = e.message ?: "Error al iniciar sesión"
                _uiState.update { it.copy(isLoadingAuth = false, authError = msg) }
                onError(msg)
            }
        }
    }

    /**
     * Restores an authenticated session (design section 9).
     *
     * It mirrors the success branch of [login]: the global inspection observation is
     * cancelled and replaced by the user-scoped one, so a restored session shows the
     * same user and the same inspections as a fresh sign-in.
     */
    fun restoreSession(user: User) {
        applyAuthenticatedUser(user)
    }

    /**
     * Applies an authenticated profile. This is the shared success branch of [login],
     * [register] and [restoreSession], so all three paths end in the same state.
     */
    private fun applyAuthenticatedUser(user: User) {
        _uiState.update {
            it.copy(
                currentUser = user,
                inspectorName = user.name,
                isLoadingAuth = false,
                authError = null,
                savedInspections = emptyList()
            )
        }
        observeUserInspections(user.email)
    }

    fun register(
        name: String,
        email: String,
        cedula: String,
        password: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedCedula = cedula.trim()
        val trimmedPassword = password.trim()

        if (trimmedName.isBlank() || trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
            val msg = "Completa todos los campos obligatorios"
            _uiState.update { it.copy(authError = msg) }
            onError(msg)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAuth = true, authError = null) }
            try {
                val existing = repository.getUserByEmail(trimmedEmail)
                if (existing != null) {
                    val msg = "Este correo ya está registrado"
                    _uiState.update { it.copy(isLoadingAuth = false, authError = msg) }
                    onError(msg)
                    return@launch
                }

                val newUser = User(
                    email = trimmedEmail,
                    name = trimmedName,
                    cedula = if (trimmedCedula.isNotBlank()) trimmedCedula else "STPS-${System.currentTimeMillis().toString().takeLast(6)}"
                )
                val created = repository.registerUser(newUser, trimmedPassword)

                // The repository returns the authoritative profile (Firebase uid included);
                // the local placeholder has no uid and would break owner-scoped sync.
                applyAuthenticatedUser(created)
                onSuccess()
            } catch (auth: AuthException) {
                val msg = authErrorMessage(auth.error, "Error al registrar cuenta")
                _uiState.update { it.copy(isLoadingAuth = false, authError = msg) }
                onError(msg)
            } catch (e: Exception) {
                val msg = e.message ?: "Error al registrar cuenta"
                _uiState.update { it.copy(isLoadingAuth = false, authError = msg) }
                onError(msg)
            }
        }
    }

    /**
     * Maps a domain auth failure to the existing Spanish copy (design section 9).
     * Provider types and raw codes never reach the UI; [fallback] is used for an
     * unmapped failure.
     */
    private fun authErrorMessage(error: AuthError, fallback: String): String = when (error) {
        AuthError.INVALID_CREDENTIALS -> "Credenciales incorrectas. Verifica tu correo y contraseña."
        AuthError.EMAIL_ALREADY_IN_USE -> "Este correo ya está registrado"
        AuthError.WEAK_PASSWORD -> "La contraseña debe tener al menos 6 caracteres."
        AuthError.NETWORK_UNAVAILABLE -> "Sin conexión a internet. Revisa tu red e inténtalo de nuevo."
        AuthError.UNKNOWN -> fallback
    }

    fun logout(onLoggedOut: () -> Unit = {}) {
        inspectionsJob?.cancel()
        _uiState.update {
            it.copy(
                currentUser = null,
                authError = null,
                savedInspections = emptyList(),
                sessionModules = emptyList(),
                sessionFindings = emptyList(),
                answers = emptyMap(),
                naAnswers = emptySet(),
                criticalStopTriggered = false
            )
        }
        observeAllInspections()
        onLoggedOut()
    }

    fun initChecklist(
        items: List<ChecklistItem>,
        restoredAnswers: Map<String, Boolean> = emptyMap(),
        restoredNA: Set<String> = emptySet()
    ) {
        val restored = restoredAnswers.isNotEmpty()
        val score = if (restored) calculateScore(items, restoredAnswers) else 0

        _uiState.update { current ->
            current.copy(
                currentChecklist = items,
                answers = restoredAnswers,
                naAnswers = restoredNA,
                overallComplianceScore = score,
                criticalStopTriggered = false,
                reinspectionDue = false
            )
        }
    }

    fun startNewSession() {
        activeInspectionId = UUID.randomUUID().toString()
        persistedIncidenceIds.clear()
        _uiState.update {
            it.copy(
                answers = emptyMap(),
                naAnswers = emptySet(),
                overallComplianceScore = 0,
                criticalStopTriggered = false,
                reinspectionDue = false,
                sessionModules = emptyList(),
                sessionFindings = emptyList(),
                errorMessage = null
            )
        }
    }

    fun openModule(moduleTitle: String, items: List<ChecklistItem>) {
        val existing = _uiState.value.sessionModules.firstOrNull { it.moduleTitle == moduleTitle }
        initChecklist(
            items = items,
            restoredAnswers = existing?.answers ?: emptyMap(),
            restoredNA = existing?.naAnswers ?: emptySet()
        )
    }

    fun setSessionContext(companyName: String, zoneName: String, auditType: String) {
        _uiState.update { current ->
            current.copy(
                companyName = companyName,
                zoneName = zoneName,
                auditType = auditType,
                answers = emptyMap(),
                naAnswers = emptySet(),
                overallComplianceScore = 0,
                criticalStopTriggered = false,
                reinspectionDue = false,
                sessionModules = emptyList(),
                sessionFindings = emptyList()
            )
        }
    }

    fun submitAnswer(item: ChecklistItem, isCompliant: Boolean, photoUri: String? = null, notes: String? = null, lat: Double? = null, lng: Double? = null) {
        val currentState = _uiState.value
        val updatedAnswers = currentState.answers.toMutableMap()
        updatedAnswers[item.id] = isCompliant

        val score = calculateScore(currentState.currentChecklist, updatedAnswers)
        val isNAAnswer = currentState.naAnswers.contains(item.id)
        val liveCriticalStop = !isCompliant && item.criticalityLevel == CriticalityLevel.HIGH && !isNAAnswer

        _uiState.update { current ->
            current.copy(
                answers = updatedAnswers,
                overallComplianceScore = score,
                criticalStopTriggered = current.criticalStopTriggered || liveCriticalStop,
                reinspectionDue = false
            )
        }

        val existing = _uiState.value.sessionFindings.firstOrNull { it.itemId == item.id }
        val isNA = _uiState.value.naAnswers.contains(item.id)
        val finalPhotoUri = if (isNA) null else photoUri ?: existing?.photoUri
        val finalNotes = if (isNA) "No aplica (N/A)" else notes ?: existing?.notes
        val finalLat = if (isNA) null else lat ?: existing?.latitude
        val finalLng = if (isNA) null else lng ?: existing?.longitude

        if (isCompliant) {
            _uiState.update {
                it.copy(sessionFindings = it.sessionFindings.filterNot { f -> f.itemId == item.id })
            }
        } else {
            val finding = SessionFinding(
                itemId = item.id,
                itemDescription = item.description,
                normCode = item.normativ.code,
                criticalityLevel = item.criticalityLevel,
                isNA = isNA,
                notes = finalNotes,
                photoUri = finalPhotoUri,
                timestamp = System.currentTimeMillis(),
                latitude = finalLat,
                longitude = finalLng
            )
            _uiState.update {
                it.copy(sessionFindings = it.sessionFindings.filterNot { f -> f.itemId == item.id } + finding)
            }
        }

        if (!isCompliant) {
            val incidenceId = persistedIncidenceIds[item.id] ?: UUID.randomUUID().toString()
            persistedIncidenceIds[item.id] = incidenceId
            viewModelScope.launch {
                try {
                    val incidence = Incidence(
                        id = incidenceId,
                        inspectionId = activeInspectionId,
                        checklistItemId = item.id,
                        status = IncidenceStatus.OPEN,
                        timestamp = System.currentTimeMillis(),
                        latitude = finalLat,
                        longitude = finalLng,
                        photoUri = finalPhotoUri,
                        notes = finalNotes
                    )
                    repository.saveIncidence(incidence)
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Error guardando incidencia: ${e.message}") }
                }
            }
        }
    }

    fun attachFindingEvidence(itemId: String, photoUri: String?, lat: Double?, lng: Double?, notes: String? = null) {
        val current = _uiState.value
        val existing = current.sessionFindings.firstOrNull { it.itemId == itemId } ?: return
        val updated = existing.copy(
            photoUri = photoUri ?: existing.photoUri,
            notes = notes ?: existing.notes,
            latitude = lat ?: existing.latitude,
            longitude = lng ?: existing.longitude,
            timestamp = System.currentTimeMillis()
        )
        _uiState.update {
            it.copy(sessionFindings = it.sessionFindings.map { f -> if (f.itemId == itemId) updated else f })
        }

        val incidenceId = persistedIncidenceIds[itemId] ?: UUID.randomUUID().toString().also {
            persistedIncidenceIds[itemId] = it
        }
        viewModelScope.launch {
            try {
                repository.saveIncidence(
                    Incidence(
                        id = incidenceId,
                        inspectionId = activeInspectionId,
                        checklistItemId = itemId,
                        status = IncidenceStatus.OPEN,
                        timestamp = updated.timestamp,
                        latitude = updated.latitude,
                        longitude = updated.longitude,
                        photoUri = updated.photoUri,
                        notes = updated.notes
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error actualizando evidencia: ${e.message}") }
            }
        }
    }

    fun markNA(itemId: String, isNA: Boolean) {
        _uiState.update {
            it.copy(naAnswers = if (isNA) it.naAnswers + itemId else it.naAnswers - itemId)
        }
    }

    fun setAnswerNA(item: ChecklistItem) {
        markNA(item.id, true)
        submitAnswer(item, isCompliant = false, photoUri = null, notes = null)
    }

    fun snapshotActiveModule(moduleTitle: String, items: List<ChecklistItem>, completed: Boolean) {
        val s = _uiState.value
        if (items.isEmpty() || (s.answers.isEmpty() && !completed)) return
        val score = calculateScore(items, s.answers)
        val critical = completed && score < REINSPECTION_THRESHOLD
        val snapshot = ModuleCompletion(
            moduleTitle = moduleTitle,
            normCodes = items.map { it.normativ.code }.distinct(),
            items = items,
            answers = s.answers,
            naAnswers = s.naAnswers,
            score = score,
            criticalStop = critical,
            reinspectionDue = critical,
            completed = completed
        )
        _uiState.update { current ->
            val existing = current.sessionModules.filterNot { it.moduleTitle == moduleTitle }
            current.copy(
                sessionModules = existing + snapshot,
                criticalStopTriggered = current.criticalStopTriggered || critical,
                reinspectionDue = current.reinspectionDue || critical
            )
        }
    }

    fun finishModule(moduleTitle: String, items: List<ChecklistItem>) {
        snapshotActiveModule(moduleTitle, items, completed = true)
    }

    fun resetSession() {
        persistedIncidenceIds.clear()
        _uiState.update {
            it.copy(
                answers = emptyMap(),
                naAnswers = emptySet(),
                overallComplianceScore = 0,
                criticalStopTriggered = false,
                reinspectionDue = false,
                sessionModules = emptyList(),
                sessionFindings = emptyList(),
                errorMessage = null
            )
        }
    }

    fun sessionScore(modules: List<ModuleCompletion>): Int {
        if (modules.isEmpty()) return 0
        return modules.map { it.score }.average().toInt()
    }

    fun computedSessionScore(): Int {
        val s = _uiState.value
        return if (s.sessionModules.isEmpty()) s.overallComplianceScore else sessionScore(s.sessionModules)
    }

    fun isReinspectionDue(score: Int): Boolean {
        return score < REINSPECTION_THRESHOLD
    }

    fun scheduleReinspection(baseTimestamp: Long): Long {
        return baseTimestamp + REINSPECTION_WINDOW_MS
    }

    /** Creates a session record, persists it in Room and returns it. */
    fun saveDictamen(): InspectionSessionRecord {
        val s = _uiState.value
        val user = s.currentUser
        val score = if (s.sessionModules.isEmpty()) s.overallComplianceScore else sessionScore(s.sessionModules)
        val locatedFinding = s.sessionFindings.lastOrNull { it.latitude != null && it.longitude != null }
        val reinspectionDue = s.sessionModules.any { it.reinspectionDue } || score < REINSPECTION_THRESHOLD
        val criticalStop = s.sessionModules.any { it.criticalStop } || s.criticalStopTriggered

        val record = InspectionSessionRecord(
            folio = generateFolio(s.savedInspections),
            inspectionId = activeInspectionId,
            userEmail = user?.email ?: "",
            companyName = s.companyName,
            zoneName = s.zoneName,
            auditType = s.auditType,
            inspectorName = user?.name ?: s.inspectorName,
            inspectorCedula = user?.cedula ?: InspectorProfile.CEDULA,
            timestamp = System.currentTimeMillis(),
            overallScore = score,
            criticalStop = criticalStop,
            reinspectionDue = reinspectionDue,
            scheduledReinspectionAt = if (reinspectionDue) scheduleReinspection(System.currentTimeMillis()) else null,
            modules = s.sessionModules,
            findings = s.sessionFindings,
            normCodes = s.sessionModules.flatMap { it.normCodes }.distinct(),
            latitude = locatedFinding?.latitude ?: DEFAULT_SITE_LATITUDE,
            longitude = locatedFinding?.longitude ?: DEFAULT_SITE_LONGITUDE,
            status = if (criticalStop || reinspectionDue) IncidenceStatus.OPEN else IncidenceStatus.CLOSED,
            // Owner identity for Firestore rules and owner-scoped queries.
            ownerUid = user?.uid.orEmpty()
        )

        // Update UI state immediately for responsive UI and test consistency
        _uiState.update {
            it.copy(savedInspections = it.savedInspections.filterNot { r -> r.folio == record.folio } + record)
        }

        // Persist to Room
        viewModelScope.launch {
            try {
                repository.saveInspection(record)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error guardando inspección: ${e.message}") }
            }
        }
        return record
    }

    fun updateRecordStatus(folio: String, status: IncidenceStatus) {
        val record = _uiState.value.savedInspections.firstOrNull { it.folio == folio } ?: return
        val updated = record.copy(status = status)
        _uiState.update {
            it.copy(savedInspections = it.savedInspections.map { r -> if (r.folio == folio) updated else r })
        }
        viewModelScope.launch {
            try {
                repository.updateInspection(updated)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error actualizando estado: ${e.message}") }
            }
        }
    }

    /** Requeues a failed inspection for another sync attempt. */
    fun retrySync(inspectionId: String) {
        viewModelScope.launch {
            try {
                repository.retrySync(inspectionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error reintentando sincronización: ${e.message}") }
            }
        }
    }

    private fun generateFolio(existing: List<InspectionSessionRecord>): String {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val sequence = existing.count { it.folio.contains("$year-") } + 1
        return "AUD-$year-${sequence.toString().padStart(4, '0')}"
    }

    internal fun calculateScore(items: List<ChecklistItem>, answers: Map<String, Boolean>): Int {
        if (items.isEmpty()) return 100
        val totalPoints = items.sumOf { it.criticalityLevel.points }
        if (totalPoints == 0) return 100
        val earnedPoints = items.filter { answers[it.id] == true }.sumOf { it.criticalityLevel.points }
        return ((earnedPoints.toDouble() / totalPoints.toDouble()) * 100).toInt()
    }

    companion object {
        const val DEFAULT_SITE_LATITUDE = 25.6866
        const val DEFAULT_SITE_LONGITUDE = -100.3161
    }
}