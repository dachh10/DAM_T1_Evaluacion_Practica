package com.example.dam_t1_evaluacion_practica.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.dam_t1_evaluacion_practica.data.local.dao.EvidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.entity.EvidenceEntity
import com.example.dam_t1_evaluacion_practica.data.mapper.toDomainModel
import com.example.dam_t1_evaluacion_practica.data.sync.EvidenceUploadWorker
import com.example.dam_t1_evaluacion_practica.domain.model.Evidence
import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceId
import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.repository.EvidenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Room-backed implementation of [EvidenceRepository].
 *
 * Evidence is the durable upload queue. Each item gets a deterministic ID
 * ([EvidenceId.compute]) so retries and re-captures address the same asset.
 *
 * On register, enqueues an [EvidenceUploadWorker] to upload to Cloudinary.
 */
class DefaultEvidenceRepository(
    private val evidenceDao: EvidenceDao,
    private val context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context)
) : EvidenceRepository {

    override fun observe(inspectionId: String): Flow<List<Evidence>> =
        evidenceDao.observeByInspection(inspectionId).map { entities ->
            entities.map { it.toDomainModel() }
        }

    override suspend fun register(
        inspectionId: String,
        itemId: String,
        localRef: String
    ): Evidence = withContext(Dispatchers.IO) {
        val evidenceId = EvidenceId.compute(inspectionId, itemId)
        val entity = EvidenceEntity(
            evidenceId = evidenceId,
            inspectionId = inspectionId,
            itemId = itemId,
            localRef = localRef,
            remoteUrl = null,
            status = EvidenceStatus.PENDING.name,
            attempts = 0,
            lastError = null
        )
        evidenceDao.upsert(entity)

        // Enqueue upload worker
        val request = OneTimeWorkRequestBuilder<EvidenceUploadWorker>()
            .setInputData(
                androidx.work.Data.Builder()
                    .putString(EvidenceUploadWorker.KEY_EVIDENCE_ID, evidenceId)
                    .build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "evidence-upload-$evidenceId",
            ExistingWorkPolicy.REPLACE,
            request
        )

        entity.toDomainModel()
    }

    override suspend fun retry(evidenceId: String) = withContext(Dispatchers.IO) {
        val existing = evidenceDao.getById(evidenceId) ?: return@withContext
        val reset = existing.copy(
            status = EvidenceStatus.PENDING.name,
            attempts = 0,
            lastError = null
        )
        evidenceDao.upsert(reset)

        // Re-enqueue upload worker
        val request = OneTimeWorkRequestBuilder<EvidenceUploadWorker>()
            .setInputData(
                androidx.work.Data.Builder()
                    .putString(EvidenceUploadWorker.KEY_EVIDENCE_ID, evidenceId)
                    .build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "evidence-upload-$evidenceId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}