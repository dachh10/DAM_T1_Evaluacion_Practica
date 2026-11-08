package com.example.dam_t1_evaluacion_practica.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Enqueues sync work for one inspection.
 *
 * This is an interface and not a concrete WorkManager wrapper so [SyncCoordinator] can be
 * exercised on the JVM: the Android implementation would drag a `Context` and a live
 * `WorkManager` into every unit test of the state machine.
 */
interface SyncScheduler {

    fun enqueueInspectionSync(inspectionId: String)
}

/**
 * Production [SyncScheduler] over WorkManager.
 *
 * Work is keyed by `inspection-sync-{inspectionId}` with `REPLACE`, so re-enqueueing the same
 * inspection replaces the pending request instead of piling up duplicate jobs. `REPLACE` also
 * resets the attempt budget on a genuine new edit (design section 6) and never on a plain retry.
 */
class WorkManagerSyncScheduler(private val context: Context) : SyncScheduler {

    // Resolved on first enqueue so app startup never pays for WorkManager's own init.
    private val workManager by lazy { WorkManager.getInstance(context.applicationContext) }

    override fun enqueueInspectionSync(inspectionId: String) {
        val request = OneTimeWorkRequestBuilder<InspectionSyncWorker>()
            .setInputData(
                Data.Builder()
                    .putString(InspectionSyncWorker.KEY_INSPECTION_ID, inspectionId)
                    .build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "inspection-sync-$inspectionId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
