package com.example.dam_t1_evaluacion_practica.data.sync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.dam_t1_evaluacion_practica.data.local.AppDatabase
import com.example.dam_t1_evaluacion_practica.data.local.entity.EvidenceEntity
import com.example.dam_t1_evaluacion_practica.data.mapper.toDomainModel
import com.example.dam_t1_evaluacion_practica.data.remote.cloudinary.CloudinaryEvidenceUploader
import com.example.dam_t1_evaluacion_practica.data.repository.DefaultEvidenceRepository
import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceStatus
import kotlinx.coroutines.runBlocking

/**
 * WorkManager worker that uploads one evidence asset to Cloudinary.
 *
 * Enqueued by [DefaultEvidenceRepository.register] when evidence is created.
 * Unique work name: `evidence-upload-{evidenceId}`.
 * Constraints: CONNECTED, exponential backoff 30s, max 5 attempts.
 * On success: persists secureUrl and marks UPLOADED.
 * On failure: increments attempts, marks FAILED after 5 attempts.
 */
class EvidenceUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        const val KEY_EVIDENCE_ID = "evidenceId"
    }

    override fun doWork(): Result {
        val evidenceId = inputData.getString(KEY_EVIDENCE_ID) ?: return Result.failure()

        val appContext = applicationContext
        val db = AppDatabase.getDatabase(appContext)
        val uploader = CloudinaryEvidenceUploader.getInstance(appContext)
        val evidenceRepository = DefaultEvidenceRepository(db.evidenceDao(), appContext)

        return runBlocking {
            try {
                val entity = db.evidenceDao().getById(evidenceId) ?: return@runBlocking Result.failure()

                // Upload to Cloudinary
                val evidence = entity.toDomainModel()
                val secureUrl = uploader.upload(evidence)

                if (secureUrl != null) {
                    // Success: update entity with secureUrl and UPLOADED status
                    val updated = entity.copy(
                        remoteUrl = secureUrl,
                        status = EvidenceStatus.UPLOADED.name,
                        attempts = entity.attempts + 1
                    )
                    db.evidenceDao().upsert(updated)
                    Result.success()
                } else {
                    // Failure: increment attempts, check if exhausted
                    val newAttempts = entity.attempts + 1
                    val failed = newAttempts >= 5
                    val updated = entity.copy(
                        status = if (failed) EvidenceStatus.FAILED.name else EvidenceStatus.PENDING.name,
                        attempts = newAttempts
                    )
                    db.evidenceDao().upsert(updated)
                    if (failed) Result.failure() else Result.retry()
                }
            } catch (e: Exception) {
                val entity = db.evidenceDao().getById(evidenceId)
                entity?.let {
                    val newAttempts = it.attempts + 1
                    val failed = newAttempts >= 5
                    val updated = it.copy(
                        status = if (failed) EvidenceStatus.FAILED.name else EvidenceStatus.PENDING.name,
                        attempts = newAttempts
                    )
                    db.evidenceDao().upsert(updated)
                }
                if (e is InterruptedException) Result.failure() else Result.retry()
            }
        }
    }
}