package com.example.dam_t1_evaluacion_practica.domain.model

import java.util.UUID

/**
 * Deterministic evidence ID generator (design section 8).
 *
 * evidenceId = UUID.nameUUIDFromBytes("$inspectionId|$itemId".toByteArray())
 *
 * Stable across retries and re-captures; used as Cloudinary public_id suffix:
 * `inspections/{inspectionId}/{evidenceId}`
 */
object EvidenceId {

    /**
     * Computes a stable UUID for an evidence asset.
     *
     * @param inspectionId the parent inspection ID
     * @param itemId the checklist item ID
     * @return a deterministic UUID v3 (name-based) as string
     */
    fun compute(inspectionId: String, itemId: String): String {
        val input = "$inspectionId|$itemId".toByteArray()
        return UUID.nameUUIDFromBytes(input).toString()
    }
}