package com.example.dam_t1_evaluacion_practica.data.remote.mapper

import com.example.dam_t1_evaluacion_practica.data.mapper.findingsToRemoteJson
import com.example.dam_t1_evaluacion_practica.data.mapper.modulesToJson
import com.example.dam_t1_evaluacion_practica.data.mapper.normCodesToJson
import com.example.dam_t1_evaluacion_practica.data.mapper.parseFindingsJson
import com.example.dam_t1_evaluacion_practica.data.mapper.parseModulesJson
import com.example.dam_t1_evaluacion_practica.data.mapper.parseNormCodesJson
import com.example.dam_t1_evaluacion_practica.domain.model.IncidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.domain.model.SyncState

/**
 * Pure conversion between an [InspectionSessionRecord] and its remote document shape
 * (`inspections/{inspectionId}`, design section 6).
 *
 * Plain Kotlin only: no Firebase types, no `org.json`. All values are JSON-compatible
 * primitives so the map can be handed straight to Firestore or read back from a
 * document snapshot without the SDK in scope.
 */
object InspectionRemoteMapper {

    const val KEY_INSPECTION_ID = "inspectionId"
    const val KEY_FOLIO = "folio"
    const val KEY_OWNER_UID = "ownerUid"
    const val KEY_USER_EMAIL = "userEmail"
    const val KEY_COMPANY_NAME = "companyName"
    const val KEY_ZONE_NAME = "zoneName"
    const val KEY_AUDIT_TYPE = "auditType"
    const val KEY_INSPECTOR_NAME = "inspectorName"
    const val KEY_INSPECTOR_CEDULA = "inspectorCedula"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_UPDATED_AT = "updatedAt"
    const val KEY_LAST_SYNCED_AT = "lastSyncedAt"
    const val KEY_OVERALL_SCORE = "overallScore"
    const val KEY_CRITICAL_STOP = "criticalStop"
    const val KEY_REINSPECTION_DUE = "reinspectionDue"
    const val KEY_SCHEDULED_REINSPECTION_AT = "scheduledReinspectionAt"
    const val KEY_STATUS = "status"
    const val KEY_LATITUDE = "latitude"
    const val KEY_LONGITUDE = "longitude"
    const val KEY_MODULES_JSON = "modulesJson"
    const val KEY_FINDINGS_JSON = "findingsJson"
    const val KEY_NORM_CODES_JSON = "normCodesJson"
    const val KEY_SYNC_STATE = "syncState"
    const val KEY_DELETED = "deleted"

    /** Builds the remote document for [record]. `deleted` is always written as `false`. */
    fun toMap(record: InspectionSessionRecord): Map<String, Any?> = mapOf(
        KEY_INSPECTION_ID to record.inspectionId,
        KEY_FOLIO to record.folio,
        KEY_OWNER_UID to record.ownerUid,
        KEY_USER_EMAIL to record.userEmail,
        KEY_COMPANY_NAME to record.companyName,
        KEY_ZONE_NAME to record.zoneName,
        KEY_AUDIT_TYPE to record.auditType,
        KEY_INSPECTOR_NAME to record.inspectorName,
        KEY_INSPECTOR_CEDULA to record.inspectorCedula,
        KEY_TIMESTAMP to record.timestamp,
        KEY_UPDATED_AT to record.updatedAt,
        KEY_LAST_SYNCED_AT to null,
        KEY_OVERALL_SCORE to record.overallScore,
        KEY_CRITICAL_STOP to record.criticalStop,
        KEY_REINSPECTION_DUE to record.reinspectionDue,
        KEY_SCHEDULED_REINSPECTION_AT to record.scheduledReinspectionAt,
        KEY_STATUS to record.status.name,
        KEY_LATITUDE to record.latitude,
        KEY_LONGITUDE to record.longitude,
        KEY_MODULES_JSON to modulesToJson(record.modules),
        KEY_FINDINGS_JSON to findingsToRemoteJson(record.findings),
        KEY_NORM_CODES_JSON to normCodesToJson(record.normCodes),
        KEY_SYNC_STATE to record.syncState.name,
        KEY_DELETED to false
    )

    /**
     * Rebuilds the domain record from a remote document. Missing keys fall back to
     * sensible defaults and unknown keys are ignored. `deleted` is dropped — it is
     * bookkeeping for the local tombstone, not part of the domain record.
     */
    fun fromMap(inspectionId: String, map: Map<String, Any?>): InspectionSessionRecord =
        InspectionSessionRecord(
            folio = map[KEY_FOLIO] as? String ?: "",
            inspectionId = inspectionId,
            userEmail = map[KEY_USER_EMAIL] as? String ?: "",
            companyName = map[KEY_COMPANY_NAME] as? String ?: "",
            zoneName = map[KEY_ZONE_NAME] as? String ?: "",
            auditType = map[KEY_AUDIT_TYPE] as? String ?: "",
            inspectorName = map[KEY_INSPECTOR_NAME] as? String ?: "",
            inspectorCedula = map[KEY_INSPECTOR_CEDULA] as? String ?: "",
            timestamp = (map[KEY_TIMESTAMP] as? Number)?.toLong() ?: 0L,
            overallScore = (map[KEY_OVERALL_SCORE] as? Number)?.toInt() ?: 0,
            criticalStop = map[KEY_CRITICAL_STOP] as? Boolean ?: false,
            reinspectionDue = map[KEY_REINSPECTION_DUE] as? Boolean ?: false,
            scheduledReinspectionAt = (map[KEY_SCHEDULED_REINSPECTION_AT] as? Number)?.toLong(),
            modules = parseModulesJson(map[KEY_MODULES_JSON] as? String ?: ""),
            findings = parseFindingsJson(map[KEY_FINDINGS_JSON] as? String ?: ""),
            normCodes = parseNormCodesJson(map[KEY_NORM_CODES_JSON] as? String ?: ""),
            latitude = (map[KEY_LATITUDE] as? Number)?.toDouble(),
            longitude = (map[KEY_LONGITUDE] as? Number)?.toDouble(),
            status = (map[KEY_STATUS] as? String)?.let { IncidenceStatus.valueOf(it) }
                ?: IncidenceStatus.OPEN,
            ownerUid = map[KEY_OWNER_UID] as? String ?: "",
            syncState = (map[KEY_SYNC_STATE] as? String)?.let { SyncState.valueOf(it) }
                ?: SyncState.LOCAL_ONLY,
            updatedAt = (map[KEY_UPDATED_AT] as? Number)?.toLong() ?: 0L
        )
}
