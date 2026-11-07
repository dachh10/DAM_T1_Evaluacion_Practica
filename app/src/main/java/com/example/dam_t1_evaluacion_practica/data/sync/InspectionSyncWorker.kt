package com.example.dam_t1_evaluacion_practica.data.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.dam_t1_evaluacion_practica.data.local.AppDatabase
import com.example.dam_t1_evaluacion_practica.data.local.dao.InspectionDao
import com.example.dam_t1_evaluacion_practica.data.remote.firestore.FirestoreInspectionDataSource
import com.example.dam_t1_evaluacion_practica.data.remote.firestore.RemoteInspectionDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * WorkManager worker that pushes one inspection to Firestore.
 *
 * The worker is enqueued by [SyncScheduler] when an inspection is saved/updated/deleted
 * locally. It reads the current Room state (so a later local edit always wins), pushes
 * to Firestore via [RemoteInspectionDataSource], and reports success/failure back to
 * [SyncCoordinator].
 */
class InspectionSyncWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val inspectionId = inputData.getString("inspectionId") ?: return Result.failure()

        val appContext = applicationContext
        val db = AppDatabase.getDatabase(appContext)

        val coordinator = SyncCoordinator(
            inspectionDao = db.inspectionDao(),
            incidenceDao = db.incidenceDao(),
            remote = FirestoreInspectionDataSource.create(),
            scheduler = SyncScheduler(appContext)
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
