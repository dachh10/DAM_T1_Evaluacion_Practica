package com.example.dam_t1_evaluacion_practica.domain.model

/**
 * Cloud replication state of a locally owned record.
 *
 * LOCAL_ONLY  persisted in Room, never queued for the cloud yet
 * PENDING     queued or in flight to Firestore
 * SYNCED      acknowledged by Firestore
 * FAILED      retry budget exhausted, manual retry required
 */
enum class SyncState {
    LOCAL_ONLY,
    PENDING,
    SYNCED,
    FAILED
}
