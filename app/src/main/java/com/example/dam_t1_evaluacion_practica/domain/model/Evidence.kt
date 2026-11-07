package com.example.dam_t1_evaluacion_practica.domain.model

import com.example.dam_t1_evaluacion_practica.domain.error.EvidenceError

/**
 * Upload lifecycle of a single evidence asset.
 */
enum class EvidenceStatus {
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED
}

/**
 * Durable record of one evidence asset attached to an inspection item.
 *
 * [localRef] is the device-local reference (file path or content:// URI) used for
 * previews and offline PDF generation. [remoteUrl] is the Cloudinary secure_url and
 * is the reference replicated into the inspection document; it stays null until the
 * upload succeeds.
 */
data class Evidence(
    val evidenceId: String,
    val inspectionId: String,
    val itemId: String,
    val localRef: String,
    val remoteUrl: String? = null,
    val status: EvidenceStatus = EvidenceStatus.PENDING,
    val attempts: Int = 0,
    val lastError: EvidenceError? = null
)
