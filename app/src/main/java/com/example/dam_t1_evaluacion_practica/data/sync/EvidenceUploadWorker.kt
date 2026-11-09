package com.example.dam_t1_evaluacion_practica.data.sync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.dam_t1_evaluacion_practica.data.local.AppDatabase
import com.example.dam_t1_evaluacion_practica.data.mapper.findingsToJson
import com.example.dam_t1_evaluacion_practica.data.mapper.parseFindingsJson
import com.example.dam_t1_evaluacion_practica.data.mapper.toDomainModel
import com.example.dam_t1_evaluacion_practica.data.remote.cloudinary.CloudinaryEvidenceUploader
import com.example.dam_t1_evaluacion_practica.data.repository.DefaultEvidenceRepository
import com.example.dam_t1_evaluacion_practica.domain.error.EvidenceError
import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.model.SyncState
import kotlinx.coroutines.runBlocking

/**
 * WorkManager worker that uploads one evidence asset to Cloudinary.
 *
 * Enqueued by [DefaultEvidenceRepository.register] when evidence is created.
 * Unique work name: `evidence-upload-{evidenceId}`.
 * Constraints: CONNECTED, exponential backoff 30s, max 5 attempts.
 * On success: persists the Cloudinary `secure_url`, marks UPLOADED and replaces the
 * device-local photo reference in the incidence and saved findings, so the cloud
 * documents carry the remote URL.
 * On failure: increments attempts, marks FAILED after 5 attempts.
 */
class EvidenceUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        const val KEY_EVIDENCE_ID = "evidenceId"
        private const val MAX_ATTEMPTS = 5
    }

    override fun doWork(): Result {
        val evidenceId = inputData.getString(KEY_EVIDENCE_ID) ?: return Result.failure()

        val appContext = applicationContext
        val db = AppDatabase.getDatabase(appContext)
        val uploader = CloudinaryEvidenceUploader.getInstance(appContext)

        return runBlocking {
            val entity = db.evidenceDao().getById(evidenceId)
                ?: return@runBlocking Result.failure()

            try {
                db.evidenceDao().upsert(entity.copy(status = EvidenceStatus.UPLOADING.name))

                val secureUrl = uploader.upload(entity.toDomainModel())

                if (secureUrl != null) {
                    db.evidenceDao().upsert(
                        entity.copy(
                            remoteUrl = secureUrl,
                            status = EvidenceStatus.UPLOADED.name,
                            attempts = entity.attempts + 1,
                            lastError = null
                        )
                    )
                    propagateRemoteUrl(db, appContext, entity.inspectionId, entity.itemId, secureUrl)
                    Result.success()
                } else {
                    val newAttempts = entity.attempts + 1
                    val failed = newAttempts >= MAX_ATTEMPTS
                    db.evidenceDao().upsert(
                        entity.copy(
                            status = if (failed) EvidenceStatus.FAILED.name else EvidenceStatus.PENDING.name,
                            attempts = newAttempts,
                            lastError = EvidenceError.UNKNOWN.name
                        )
                    )
                    if (failed) Result.failure() else Result.retry()
                }
            } catch (e: Exception) {
                val current = db.evidenceDao().getById(evidenceId)
                current?.let {
                    val newAttempts = it.attempts + 1
                    val failed = newAttempts >= MAX_ATTEMPTS
                    db.evidenceDao().upsert(
                        it.copy(
                            status = if (failed) EvidenceStatus.FAILED.name else EvidenceStatus.PENDING.name,
                            attempts = newAttempts,
                            lastError = EvidenceError.UNKNOWN.name
                        )
                    )
                }
                if (e is InterruptedException) Result.failure() else Result.retry()
            }
        }
    }

    /**
     * Replaces the local `content://` photo with the Cloudinary URL in the incidence row
     * and in the inspection findings, then re-enqueues the inspection sync so Firestore
     * receives the remote reference.
     */
    private suspend fun propagateRemoteUrl(
        db: AppDatabase,
        context: Context,
        inspectionId: String,
        itemId: String,
        remoteUrl: String
    ) {
        db.incidenceDao().findIncidencesByInspection(inspectionId)
            .filter { it.checklistItemId == itemId }
            .forEach { incidence ->
                db.incidenceDao().updateIncidence(
                    incidence.copy(
                        photoUri = remoteUrl,
                        syncState = SyncState.PENDING.name
                    )
                )
            }

        val inspection = db.inspectionDao().findInspectionById(inspectionId) ?: return
        val findings = parseFindingsJson(inspection.findingsJson)
        val updatedFindingsJson = if (findings.any { it.itemId == itemId }) {
            findingsToJson(
                findings.map { finding ->
                    if (finding.itemId == itemId) finding.copy(photoUri = remoteUrl) else finding
                }
            )
        } else {
            inspection.findingsJson
        }

        db.inspectionDao().updateInspection(
            inspection.copy(
                findingsJson = updatedFindingsJson,
                syncState = SyncState.PENDING.name
            )
        )
        WorkManagerSyncScheduler(context).enqueueInspectionSync(inspectionId)
    }
}
