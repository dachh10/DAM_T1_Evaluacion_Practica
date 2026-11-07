package com.example.dam_t1_evaluacion_practica.data.repository

import com.example.dam_t1_evaluacion_practica.data.local.dao.EvidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.entity.EvidenceEntity
import com.example.dam_t1_evaluacion_practica.data.mapper.toDomainModel
import com.example.dam_t1_evaluacion_practica.domain.model.Evidence
import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceId
import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.repository.EvidenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed implementation of [EvidenceRepository].
 *
 * Evidence is the durable upload queue. Each item gets a deterministic ID
 * ([EvidenceId.compute]) so retries and re-captures address the same asset.
 */
class DefaultEvidenceRepository(
    private val evidenceDao: EvidenceDao
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
    }
}