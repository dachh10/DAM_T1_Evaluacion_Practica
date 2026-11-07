package com.example.dam_t1_evaluacion_practica.data.sync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.dam_t1_evaluacion_practica.data.local.AppDatabase
import com.example.dam_t1_evaluacion_practica.data.remote.firestore.FirestoreInspectionDataSource
import kotlinx.coroutines.runBlocking

/**
 * WorkManager worker that pushes one inspection to Firestore.
 *
 * Registered by [WorkManagerSyncScheduler] whenever an inspection is created, edited or
 * tombstoned locally. The worker reads the *current* Room state, so a later local edit is
 * always what reaches Firestore, and reports the outcome back to [SyncCoordinator].
 *
 * This class is deliberately thin glue: the state machine it drives is unit-tested through
 * [SyncCoordinator], which does not need an Android runtime.
 */
class InspectionSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        const val KEY_INSPECTION_ID = "inspectionId"
    }

    override fun doWork(): Result {
        val inspectionId = inputData.getString(KEY_INSPECTION_ID) ?: return Result.failure()

        val appContext = applicationContext
        val db = AppDatabase.getDatabase(appContext)

        val coordinator = SyncCoordinator(
            inspectionDao = db.inspectionDao(),
            incidenceDao = db.incidenceDao(),
            remote = FirestoreInspectionDataSource.create(),
            scheduler = WorkManagerSyncScheduler(appContext)
        )

        return runBlocking {
            try {
                coordinator.syncInspection(inspectionId)
                Result.success()
            } catch (e: Exception) {
                coordinator.markFailed(inspectionId)
                Result.failure()
            }
        }
    }
}
