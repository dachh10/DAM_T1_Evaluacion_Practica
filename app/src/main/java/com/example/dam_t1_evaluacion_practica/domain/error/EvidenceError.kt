package com.example.dam_t1_evaluacion_practica.domain.error

/**
 * Evidence upload failures mapped by the Cloudinary adapter.
 * Only the enum name is persisted in the evidence record's lastError column.
 */
enum class EvidenceError {
    NETWORK,
    REJECTED,
    TOO_LARGE,
    STORAGE_IO,
    EXHAUSTED,
    UNKNOWN
}
