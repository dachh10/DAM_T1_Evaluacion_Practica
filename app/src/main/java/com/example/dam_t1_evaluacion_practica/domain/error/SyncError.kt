package com.example.dam_t1_evaluacion_practica.domain.error

/**
 * Inspection sync failures mapped by the remote adapters.
 * Only the enum name is persisted in the local record's lastError column.
 */
enum class SyncError {
    NETWORK,
    PERMISSION_DENIED,
    NOT_FOUND,
    INVALID_DATA,
    EXHAUSTED,
    UNKNOWN
}
