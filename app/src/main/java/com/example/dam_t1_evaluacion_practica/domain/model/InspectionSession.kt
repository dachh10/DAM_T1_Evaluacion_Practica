package com.example.dam_t1_evaluacion_practica.domain.model

/**
 * In-memory snapshot of a finished (or in-progress) checklist module.
 * Stored at session level so Results / Detail / Historial can render a
 * per-module breakdown without touching the pure scoring rules.
 */
data class ModuleCompletion(
    val moduleTitle: String,
    val normCodes: List<String>,
    val items: List<ChecklistItem>,
    val answers: Map<String, Boolean>,
    val naAnswers: Set<String>,
    val score: Int,
    val criticalStop: Boolean,
    val reinspectionDue: Boolean,
    val completed: Boolean
) {
    val answeredCount: Int get() = answers.size
    val totalCount: Int get() = items.size
}

/**
 * In-memory finding (non-compliant item) tracked during the current session.
 * N/A answers are stored as non-compliant with isNA = true for visual distinction.
 */
data class SessionFinding(
    val itemId: String,
    val itemDescription: String,
    val normCode: String,
    val criticalityLevel: CriticalityLevel,
    val isNA: Boolean,
    val notes: String?,
    val photoUri: String?,
    val timestamp: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Mirrors [ChecklistItem.requiresPhotoEvidence]: the point itself mandates a photo. */
    val requiresPhotoEvidence: Boolean = false
)

/**
 * Immutable record created when the inspector saves the dictamen.
 * Persisted in Room via mappers.
 */
data class InspectionSessionRecord(
    val folio: String,
    // Cloud identity assigned at session start; folio stays display-only.
    val inspectionId: String,
    /** Set when this dictamen re-evaluates an earlier critical inspection. */
    val parentInspectionId: String? = null,
    val userEmail: String = "",
    val companyName: String,
    val zoneName: String,
    val auditType: String,
    val inspectorName: String,
    val inspectorCedula: String,
    val timestamp: Long,
    val overallScore: Int,
    val criticalStop: Boolean,
    val reinspectionDue: Boolean,
    val scheduledReinspectionAt: Long? = null,
    val modules: List<ModuleCompletion>,
    val findings: List<SessionFinding>,
    val normCodes: List<String>,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: IncidenceStatus = IncidenceStatus.OPEN,
    val ownerUid: String = "",
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val updatedAt: Long = timestamp
)
