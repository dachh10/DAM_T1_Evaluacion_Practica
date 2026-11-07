package com.example.dam_t1_evaluacion_practica.data.remote.mapper

import com.example.dam_t1_evaluacion_practica.domain.model.Incidence
import com.example.dam_t1_evaluacion_practica.domain.model.IncidenceStatus

/**
 * Pure conversion between an [Incidence] and its remote document shape
 * (`incidences/{inspectionId}/{incidenceId}`, design section 6).
 *
 * Plain Kotlin only: no Firebase types, no `org.json`. A local `content://` photo
 * reference never reaches the cloud — it is written as `null`; an already-uploaded
 * `https://` URL is preserved.
 */
object IncidenceRemoteMapper {

    const val KEY_ID = "id"
    const val KEY_INSPECTION_ID = "inspectionId"
    const val KEY_CHECKLIST_ITEM_ID = "checklistItemId"
    const val KEY_STATUS = "status"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_UPDATED_AT = "updatedAt"
    const val KEY_LATITUDE = "latitude"
    const val KEY_LONGITUDE = "longitude"
    const val KEY_PHOTO_URI = "photoUri"
    const val KEY_NOTES = "notes"
    const val KEY_SYNC_STATE = "syncState"
    const val KEY_DELETED = "deleted"

    /** Builds the remote document for [incidence]. `deleted` is always written as `false`. */
    fun toMap(incidence: Incidence): Map<String, Any?> = mapOf(
        KEY_ID to incidence.id,
        KEY_INSPECTION_ID to incidence.inspectionId,
        KEY_CHECKLIST_ITEM_ID to incidence.checklistItemId,
        KEY_STATUS to incidence.status.name,
        KEY_TIMESTAMP to incidence.timestamp,
        KEY_UPDATED_AT to incidence.timestamp,
        KEY_LATITUDE to incidence.latitude,
        KEY_LONGITUDE to incidence.longitude,
        KEY_PHOTO_URI to if (incidence.photoUri?.startsWith("content://") == true) null else incidence.photoUri,
        KEY_NOTES to incidence.notes,
        KEY_SYNC_STATE to "LOCAL_ONLY",
        KEY_DELETED to false
    )

    /**
     * Rebuilds the domain incidence from a remote document. Missing keys fall back to
     * sensible defaults and unknown keys are ignored.
     */
    fun fromMap(id: String, map: Map<String, Any?>): Incidence = Incidence(
        id = id,
        inspectionId = map[KEY_INSPECTION_ID] as? String ?: "",
        checklistItemId = map[KEY_CHECKLIST_ITEM_ID] as? String ?: "",
        status = (map[KEY_STATUS] as? String)?.let { IncidenceStatus.valueOf(it) }
            ?: IncidenceStatus.OPEN,
        timestamp = (map[KEY_TIMESTAMP] as? Number)?.toLong() ?: 0L,
        latitude = (map[KEY_LATITUDE] as? Number)?.toDouble(),
        longitude = (map[KEY_LONGITUDE] as? Number)?.toDouble(),
        photoUri = map[KEY_PHOTO_URI] as? String,
        notes = map[KEY_NOTES] as? String
    )
}
