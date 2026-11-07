package com.example.dam_t1_evaluacion_practica.data.sync

import android.content.Context
import com.example.dam_t1_evaluacion_practica.data.local.dao.IncidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.InspectionDao
import com.example.dam_t1_evaluacion_practica.data.remote.firestore.RemoteInspectionDataSource
import com.example.dam_t1_evaluacion_practica.data.local.entity.InspectionEntity
import com.example.dam_t1_evaluacion_practica.domain.model.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owns the per-inspection sync state machine (design section 6):
 *
 *   LOCAL_ONLY ──save/update/delete──► PENDING ──worker ok──► SYNCED
 *                                      │                     │
 *                                      └─attempts exhausted──► FAILED ──retrySync──► PENDING
 *
 * The coordinator is the single writer of syncState in Room. The worker
 * ([InspectionSyncWorker]) re-reads current Room state before pushing, so a later
 * local edit is always what gets pushed to Firestore.
 */
class SyncCoordinator(
    private val inspectionDao: InspectionDao,
    private val incidenceDao: IncidenceDao,
    private val remote: RemoteInspectionDataSource,
    private val scheduler: SyncScheduler
) {

    /** Marks an inspection PENDING and enqueues sync work. Called from the UI thread. */
    suspend fun markPending(inspectionId: String) = withContext(Dispatchers.IO) {
        val entity = inspectionDao.findInspectionById(inspectionId) ?: return@withContext
        inspectionDao.updateInspection(entity.copy(syncState = SyncState.PENDING.name))
        scheduler.enqueueInspectionSync(inspectionId)
    }

    /** Pushes one inspection to Firestore and updates its sync state accordingly. */
    suspend fun syncInspection(inspectionId: String) = withContext(Dispatchers.IO) {
        val entity = inspectionDao.findInspectionById(inspectionId) ?: return@withContext

        // 1. Push parent document first (Firestore rules see pre-batch state).
        remote.saveInspection(entity)

        // 2. Batch-push all incidences under the parent.
        val incidences = incidenceDao.findIncidencesByInspection(inspectionId)
        remote.saveIncidences(inspectionId, incidences)

        // 3. Mark SYNCED and record the sync timestamp.
        inspectionDao.updateInspection(
            entity.copy(
                syncState = SyncState.SYNCED.name,
                lastSyncedAt = System.currentTimeMillis()
            )
        )
    }

    /** Marks an inspection FAILED (worker calls this after exhausting retries). */
    suspend fun markFailed(inspectionId: String) = withContext(Dispatchers.IO) {
        val entity = inspectionDao.findInspectionById(inspectionId) ?: return@withContext
        inspectionDao.updateInspection(entity.copy(syncState = SyncState.FAILED.name))
    }
}
