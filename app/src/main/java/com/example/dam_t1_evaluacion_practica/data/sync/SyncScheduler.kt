package com.example.dam_t1_evaluacion_practica.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.dam_t1_evaluacion_practica.data.sync.InspectionSyncWorker
import java.util.concurrent.TimeUnit

/**
 * Enqueues unique, network-constrained sync work for one inspection.
 *
 * Work is keyed by `inspection-sync-{inspectionId}` so re-enqueuing the same
 * inspection replaces the previous request (no duplicate sync jobs pile up).
 */
class SyncScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun enqueueInspectionSync(inspectionId: String) {
        val request = OneTimeWorkRequestBuilder<InspectionSyncWorker>()
            .setInputData(
                androidx.work.Data.Builder()
                    .putString("inspectionId", inspectionId)
                    .build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("inspection-sync-$inspectionId", ExistingWorkPolicy.REPLACE, request)
    }
}
