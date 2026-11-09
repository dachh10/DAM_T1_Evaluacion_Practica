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

    /** Display name of the authenticated inspector, falling back to the demo identity. */
    fun nameOf(user: User?): String =
        user?.name?.trim()?.takeIf { it.isNotEmpty() } ?: NAME

    /** Cédula of the authenticated inspector, falling back to the demo identity. */
    fun cedulaOf(user: User?): String =
        user?.cedula?.trim()?.takeIf { it.isNotEmpty() } ?: CEDULA

    /** Avatar initials derived from the authenticated name; falls back to the demo initials. */
    fun initialsOf(user: User?): String = nameOf(user)
        .split(" ")
        .filter { it.isNotBlank() && !it.startsWith("Ing", ignoreCase = true) && !it.startsWith("Lic", ignoreCase = true) }
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .take(2)
        .joinToString("")
        .ifBlank { INITIALS }
}

data class InspectionUiState(
    val currentChecklist: List<ChecklistItem> = emptyList(),
    val answers: Map<String, Boolean> = emptyMap(),
    val overallComplianceScore: Int = 0,
    val criticalStopTriggered: Boolean = false,
    val reinspectionDue: Boolean = false,
    /** True once the inspector explicitly confirms the work stoppage in the dialog. */
    val criticalStopAcknowledged: Boolean = false,
    /** Title of the module currently open, so snapshot edits target the right module. */
    val currentModuleTitle: String? = null,
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
    val savedInspections: List<InspectionSessionRecord> = emptyList(),
    // --- Reinspection link ---
    /** Dictamen crítico que esta sesión vuelve a evaluar, si aplica. */
    val parentInspectionId: String? = null,
    val parentInspectionFolio: String? = null
) {
    /**
     * Item ids of the session findings that still lack their mandatory evidence:
     *
     * - every HIGH finding requires a note (critical stop rule) and a photo when its
     *   checklist point has `requiresPhotoEvidence = true`;
     * - a MEDIUM/LOW finding that declares `requiresPhotoEvidence` requires a photo;
     * - N/A answers never require evidence.
     *
     * The UI locks those rows and disables "Finalizar Módulo" while any id remains
     * pending so a finding can never be saved without the evidence its rule demands.
     */
    val criticalPendingItemIds: Set<String>
        get() = sessionFindings.filter { it.isEvidenceIncomplete() }.map { it.itemId }.toSet()

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

    /**
     * A critical stop halts the walkthrough: no new answers are accepted until the
     * inspector explicitly confirms the work stoppage in the Critical Stop dialog.
     */
    val answersLocked: Boolean
        get() = criticalStopTriggered && !criticalStopAcknowledged
}

/**
 * Evidence each finding still owes, honoring the checklist point's own
 * `requiresPhotoEvidence` flag instead of assuming every HIGH point needs a photo.
 */
private fun SessionFinding.isEvidenceIncomplete(): Boolean {
    if (isNA) return false
    // The photo is mandated by the point itself (`requiresPhotoEvidence`); the note is
    // mandated by the critical-stop rule for HIGH findings.
    val needsPhoto = requiresPhotoEvidence
    val needsNote = criticalityLevel == CriticalityLevel.HIGH
    return (needsPhoto && photoUri.isNullOrBlank()) || (needsNote && notes.isNullOrBlank())
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
                criticalStopTriggered = false,
                criticalStopAcknowledged = false,
                currentModuleTitle = null,
                parentInspectionId = null,
                parentInspectionFolio = null
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
        val score = if (restored) calculateScore(items, restoredAnswers, restoredNA) else 0

        _uiState.update { current ->
            // Opening another module must not clear a stop that belongs to the session;
            // it is re-derived from the findings and the modules already recorded.
            val flags = deriveSessionFlags(
                items = items,
                answers = restoredAnswers,
                naAnswers = restoredNA,
                findings = current.sessionFindings,
                modules = current.sessionModules
            )
            current.copy(
                currentChecklist = items,
                answers = restoredAnswers,
                naAnswers = restoredNA,
                overallComplianceScore = score,
                currentModuleTitle = null,
                criticalStopTriggered = flags.criticalStop,
                reinspectionDue = flags.reinspectionDue
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
                criticalStopAcknowledged = false,
                currentModuleTitle = null,
                reinspectionDue = false,
                sessionModules = emptyList(),
                sessionFindings = emptyList(),
                parentInspectionId = null,
                parentInspectionFolio = null,
                errorMessage = null
            )
        }
    }

    /**
     * Prepares a fresh session that re-evaluates [parent] (a critical or
     * reinspection-due dictamen). The company/zone/audit type are inherited so the
     * inspector does not retype them; saving the session updates that same dictamen
     * instead of appending a new one.
     *
     * The closing status does not gate this: a critical dictamen can be re-evaluated
     * even when it was already moved to mitigation or closed.
     */
    fun startReinspection(parent: InspectionSessionRecord) {
        _uiState.update {
            it.copy(
                companyName = parent.companyName,
                zoneName = parent.zoneName,
                auditType = parent.auditType
            )
        }
        startNewSession()
        // The re-evaluation belongs to the same inspection, so incidences captured now
        // stay linked to the record that is being updated in place.
        activeInspectionId = parent.inspectionId
        _uiState.update {
            it.copy(
                parentInspectionId = parent.inspectionId,
                parentInspectionFolio = parent.folio
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
        _uiState.update { it.copy(currentModuleTitle = moduleTitle) }
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
                criticalStopAcknowledged = false,
                currentModuleTitle = null,
                reinspectionDue = false,
                sessionModules = emptyList(),
                sessionFindings = emptyList(),
                parentInspectionId = null,
                parentInspectionFolio = null
            )
        }
    }

    fun submitAnswer(item: ChecklistItem, isCompliant: Boolean, photoUri: String? = null, notes: String? = null, lat: Double? = null, lng: Double? = null) {
        val currentState = _uiState.value
        val updatedAnswers = currentState.answers.toMutableMap()
        updatedAnswers[item.id] = isCompliant

        val existing = currentState.sessionFindings.firstOrNull { it.itemId == item.id }
        val isNA = currentState.naAnswers.contains(item.id)
        val finalPhotoUri = if (isNA) null else photoUri ?: existing?.photoUri
        val finalNotes = if (isNA) "No aplica (N/A)" else notes ?: existing?.notes
        val finalLat = if (isNA) null else lat ?: existing?.latitude
        val finalLng = if (isNA) null else lng ?: existing?.longitude

        val updatedFindings = if (isCompliant) {
            currentState.sessionFindings.filterNot { f -> f.itemId == item.id }
        } else {
            currentState.sessionFindings.filterNot { f -> f.itemId == item.id } + SessionFinding(
                itemId = item.id,
                itemDescription = item.description,
                normCode = item.normativ.code,
                criticalityLevel = item.criticalityLevel,
                isNA = isNA,
                notes = finalNotes,
                photoUri = finalPhotoUri,
                timestamp = System.currentTimeMillis(),
                latitude = finalLat,
                longitude = finalLng,
                requiresPhotoEvidence = item.requiresPhotoEvidence
            )
        }

        // Keep an already-recorded module snapshot in sync when the inspector edits a
        // point after returning to the module, so the consolidated score never goes stale.
        val updatedModules = updateModuleSnapshot(
            modules = currentState.sessionModules,
            moduleTitle = currentState.currentModuleTitle,
            item = item,
            answer = isCompliant,
            naAnswers = currentState.naAnswers
        )

        val score = calculateScore(currentState.currentChecklist, updatedAnswers, currentState.naAnswers)
        val liveCriticalStop = !isCompliant && item.criticalityLevel == CriticalityLevel.HIGH && !isNA
        val flags = deriveSessionFlags(
            items = currentState.currentChecklist,
            answers = updatedAnswers,
            naAnswers = currentState.naAnswers,
            findings = updatedFindings,
            modules = updatedModules
        )

        _uiState.update { current ->
            current.copy(
                answers = updatedAnswers,
                overallComplianceScore = score,
                sessionFindings = updatedFindings,
                sessionModules = updatedModules,
                criticalStopTriggered = flags.criticalStop,
                reinspectionDue = flags.reinspectionDue,
                // A fresh live stop re-arms the explicit work-stoppage confirmation.
                criticalStopAcknowledged = if (liveCriticalStop) false else current.criticalStopAcknowledged
            )
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

        // Every newly captured photo becomes a durable evidence upload: the worker
        // uploads it to Cloudinary and replaces the local content:// reference with
        // the returned secure_url before the inspection is synced to Firestore.
        val shouldRegisterEvidence = !isCompliant &&
            !isNA &&
            !finalPhotoUri.isNullOrBlank() &&
            finalPhotoUri != existing?.photoUri
        if (shouldRegisterEvidence) {
            val photoRef = finalPhotoUri!!
            viewModelScope.launch {
                try {
                    repository.registerEvidence(activeInspectionId, item.id, photoRef)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = "Error registrando evidencia: ${e.message}")
                    }
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
        val newPhotoRef = updated.photoUri?.takeIf { it.isNotBlank() }
        val photoChanged = newPhotoRef != null && newPhotoRef != existing.photoUri
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
                if (photoChanged) {
                    repository.registerEvidence(activeInspectionId, itemId, newPhotoRef)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error actualizando evidencia: ${e.message}") }
            }
        }
    }

    /**
     * Explicit confirmation of the work stoppage. Until this is called, [InspectionUiState.answersLocked]
     * keeps the walkthrough frozen so a critical finding cannot be buried under new answers.
     */
    fun acknowledgeCriticalStop() {
        _uiState.update { it.copy(criticalStopAcknowledged = true) }
    }

    /**
     * Lets the inspector fix a HIGH point answered as non-compliant by mistake.
     *
     * The correction is deliberate (the UI asks for confirmation first), removes the
     * finding, recomputes the module and session flags and closes the persisted
     * incidence so the correction also reaches Firestore.
     */
    fun correctCriticalAnswer(item: ChecklistItem) {
        val current = _uiState.value
        val updatedAnswers = current.answers.toMutableMap().apply { put(item.id, true) }
        val updatedFindings = current.sessionFindings.filterNot { it.itemId == item.id }
        val updatedModules = updateModuleSnapshot(
            modules = current.sessionModules,
            moduleTitle = current.currentModuleTitle,
            item = item,
            answer = true,
            naAnswers = current.naAnswers
        )
        val score = calculateScore(current.currentChecklist, updatedAnswers, current.naAnswers)
        val flags = deriveSessionFlags(
            items = current.currentChecklist,
            answers = updatedAnswers,
            naAnswers = current.naAnswers,
            findings = updatedFindings,
            modules = updatedModules
        )
        _uiState.update {
            it.copy(
                answers = updatedAnswers,
                overallComplianceScore = score,
                sessionFindings = updatedFindings,
                sessionModules = updatedModules,
                criticalStopTriggered = flags.criticalStop,
                reinspectionDue = flags.reinspectionDue
            )
        }

        val incidenceId = persistedIncidenceIds[item.id] ?: return
        viewModelScope.launch {
            try {
                repository.saveIncidence(
                    Incidence(
                        id = incidenceId,
                        inspectionId = activeInspectionId,
                        checklistItemId = item.id,
                        status = IncidenceStatus.CLOSED,
                        timestamp = System.currentTimeMillis(),
                        latitude = null,
                        longitude = null,
                        photoUri = null,
                        notes = "Respuesta corregida a conforme"
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error corrigiendo incidencia: ${e.message}") }
            }
        }
    }

    /**
     * Recomputes a recorded module snapshot after one of its points changes, so the
     * consolidated zone score and the module's own stop flags stay coherent.
     */
    private fun updateModuleSnapshot(
        modules: List<ModuleCompletion>,
        moduleTitle: String?,
        item: ChecklistItem,
        answer: Boolean,
        naAnswers: Set<String>
    ): List<ModuleCompletion> = modules.map { module ->
        if (moduleTitle == null || module.moduleTitle != moduleTitle) return@map module
        if (module.items.none { it.id == item.id }) return@map module
        val answers = module.answers.toMutableMap().apply { put(item.id, answer) }
        val score = calculateScore(module.items, answers, naAnswers)
        val low = module.completed && score < REINSPECTION_THRESHOLD
        module.copy(
            answers = answers,
            naAnswers = naAnswers,
            score = score,
            criticalStop = low,
            reinspectionDue = low
        )
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
        val score = calculateScore(items, s.answers, s.naAnswers)
        val low = completed && score < REINSPECTION_THRESHOLD
        val snapshot = ModuleCompletion(
            moduleTitle = moduleTitle,
            normCodes = items.map { it.normativ.code }.distinct(),
            items = items,
            answers = s.answers,
            naAnswers = s.naAnswers,
            score = score,
            criticalStop = low,
            reinspectionDue = low,
            completed = completed
        )
        val updatedModules = s.sessionModules.filterNot { it.moduleTitle == moduleTitle } + snapshot
        val flags = deriveSessionFlags(
            items = items,
            answers = s.answers,
            naAnswers = s.naAnswers,
            findings = s.sessionFindings,
            modules = updatedModules
        )
        _uiState.update { current ->
            current.copy(
                sessionModules = updatedModules,
                currentModuleTitle = moduleTitle,
                criticalStopTriggered = flags.criticalStop,
                reinspectionDue = flags.reinspectionDue
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
                criticalStopAcknowledged = false,
                currentModuleTitle = null,
                reinspectionDue = false,
                sessionModules = emptyList(),
                sessionFindings = emptyList(),
                parentInspectionId = null,
                parentInspectionFolio = null,
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
        val completed = s.sessionModules.filter { it.completed }
        return if (completed.isEmpty()) s.overallComplianceScore else consolidatedScore(completed)
    }

    fun isReinspectionDue(score: Int): Boolean {
        return score < REINSPECTION_THRESHOLD
    }

    fun scheduleReinspection(baseTimestamp: Long): Long {
        return baseTimestamp + REINSPECTION_WINDOW_MS
    }

    /** Creates or re-evaluates a session record, persists it in Room and returns it. */
    fun saveDictamen(): InspectionSessionRecord {
        val s = _uiState.value
        val user = s.currentUser
        val completedModules = s.sessionModules.filter { it.completed }
        val score = if (completedModules.isEmpty()) s.overallComplianceScore else consolidatedScore(completedModules)
        val locatedFinding = s.sessionFindings.lastOrNull { it.latitude != null && it.longitude != null }
        val reinspectionDue = s.reinspectionDue || score < REINSPECTION_THRESHOLD
        val criticalStop = s.criticalStopTriggered || score < REINSPECTION_THRESHOLD
        val now = System.currentTimeMillis()

        // A reinspection re-evaluates the SAME dictamen: the record is updated in place
        // (same folio and inspectionId) instead of appending a new one, so history never
        // grows with a duplicate after re-evaluating a critical stop.
        val target = s.parentInspectionId
            ?.let { id -> s.savedInspections.firstOrNull { it.inspectionId == id } }

        val record = if (target != null) {
            target.copy(
                companyName = s.companyName,
                zoneName = s.zoneName,
                auditType = s.auditType,
                inspectorName = user?.name ?: s.inspectorName,
                inspectorCedula = user?.cedula ?: InspectorProfile.CEDULA,
                userEmail = user?.email?.takeIf { it.isNotBlank() } ?: target.userEmail,
                ownerUid = user?.uid?.takeIf { it.isNotBlank() } ?: target.ownerUid,
                timestamp = now,
                overallScore = score,
                criticalStop = criticalStop,
                reinspectionDue = reinspectionDue,
                scheduledReinspectionAt = if (reinspectionDue) scheduleReinspection(now) else null,
                modules = s.sessionModules,
                findings = s.sessionFindings,
                normCodes = s.sessionModules.flatMap { it.normCodes }.distinct(),
                latitude = locatedFinding?.latitude ?: DEFAULT_SITE_LATITUDE,
                longitude = locatedFinding?.longitude ?: DEFAULT_SITE_LONGITUDE,
                status = if (criticalStop || reinspectionDue) IncidenceStatus.OPEN else IncidenceStatus.CLOSED,
                updatedAt = now
            )
        } else {
            InspectionSessionRecord(
                folio = generateFolio(s.savedInspections),
                inspectionId = activeInspectionId,
                parentInspectionId = s.parentInspectionId,
                userEmail = user?.email ?: "",
                companyName = s.companyName,
                zoneName = s.zoneName,
                auditType = s.auditType,
                inspectorName = user?.name ?: s.inspectorName,
                inspectorCedula = user?.cedula ?: InspectorProfile.CEDULA,
                timestamp = now,
                overallScore = score,
                criticalStop = criticalStop,
                reinspectionDue = reinspectionDue,
                scheduledReinspectionAt = if (reinspectionDue) scheduleReinspection(now) else null,
                modules = s.sessionModules,
                findings = s.sessionFindings,
                normCodes = s.sessionModules.flatMap { it.normCodes }.distinct(),
                latitude = locatedFinding?.latitude ?: DEFAULT_SITE_LATITUDE,
                longitude = locatedFinding?.longitude ?: DEFAULT_SITE_LONGITUDE,
                status = if (criticalStop || reinspectionDue) IncidenceStatus.OPEN else IncidenceStatus.CLOSED,
                // Owner identity for Firestore rules and owner-scoped queries.
                ownerUid = user?.uid.orEmpty()
            )
        }

        // Update UI state immediately for responsive UI and test consistency.
        _uiState.update {
            val records = if (target != null) {
                it.savedInspections.map { r -> if (r.inspectionId == record.inspectionId) record else r }
            } else {
                it.savedInspections.filterNot { r -> r.folio == record.folio } + record
            }
            it.copy(savedInspections = records)
        }

        // Persist to Room: an in-place re-evaluation updates the existing row.
        viewModelScope.launch {
            try {
                if (target != null) repository.updateInspection(record) else repository.saveInspection(record)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error guardando inspección: ${e.message}") }
            }
        }
        return record
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

    /**
     * Weighted compliance over the applicable points only: N/A answers are removed from
     * both numerator and denominator, so "no aplica" never lowers the zone score.
     */
    internal fun calculateScore(
        items: List<ChecklistItem>,
        answers: Map<String, Boolean>,
        naAnswers: Set<String> = emptySet()
    ): Int {
        val applicable = items.filterNot { it.id in naAnswers }
        if (applicable.isEmpty()) return 100
        val totalPoints = applicable.sumOf { it.criticalityLevel.points }
        if (totalPoints == 0) return 100
        val earnedPoints = applicable.filter { answers[it.id] == true }.sumOf { it.criticalityLevel.points }
        return ((earnedPoints.toDouble() / totalPoints.toDouble()) * 100).toInt()
    }

    /**
     * Consolidated (zone-level) compliance across completed modules. Weighted by each
     * point's criticality, excluding N/A answers, because the 85% rule applies to the
     * whole inspected zone and not only to one module at a time.
     */
    fun consolidatedScore(modules: List<ModuleCompletion>): Int {
        val applicable = modules.flatMap { module ->
            module.items.filterNot { it.id in module.naAnswers }.map { item -> item to module }
        }
        if (applicable.isEmpty()) return 100
        val totalPoints = applicable.sumOf { (item, _) -> item.criticalityLevel.points }
        if (totalPoints == 0) return 100
        val earnedPoints = applicable
            .filter { (item, module) -> module.answers[item.id] == true }
            .sumOf { (item, _) -> item.criticalityLevel.points }
        return ((earnedPoints.toDouble() / totalPoints.toDouble()) * 100).toInt()
    }

    private data class SessionFlags(val criticalStop: Boolean, val reinspectionDue: Boolean)

    /**
     * Single source of truth for the two session flags:
     *
     * - critical stop: any live HIGH finding, any module that closed below 85, the
     *   consolidated zone score below 85, or a fully answered module below 85;
     * - reinspection: any module below 85, the consolidated zone below 85, or a fully
     *   answered module below 85 (no longer waits for "Finalizar Módulo").
     */
    private fun deriveSessionFlags(
        items: List<ChecklistItem>,
        answers: Map<String, Boolean>,
        naAnswers: Set<String>,
        findings: List<SessionFinding>,
        modules: List<ModuleCompletion>
    ): SessionFlags {
        val liveCritical = findings.any { !it.isNA && it.criticalityLevel == CriticalityLevel.HIGH }
        val moduleCritical = modules.any { it.criticalStop }
        val moduleReinspection = modules.any { it.reinspectionDue }
        val completedModules = modules.filter { it.completed }
        val zoneLow = completedModules.isNotEmpty() &&
            consolidatedScore(completedModules) < REINSPECTION_THRESHOLD
        val applicable = items.filterNot { it.id in naAnswers }
        val moduleComplete = applicable.isNotEmpty() && applicable.all { answers.containsKey(it.id) }
        val moduleLow = moduleComplete && calculateScore(items, answers, naAnswers) < REINSPECTION_THRESHOLD
        return SessionFlags(
            criticalStop = liveCritical || moduleCritical || zoneLow || moduleLow,
            reinspectionDue = moduleReinspection || zoneLow || moduleLow
        )
    }

    companion object {
        const val DEFAULT_SITE_LATITUDE = 25.6866
        const val DEFAULT_SITE_LONGITUDE = -100.3161
    }
}