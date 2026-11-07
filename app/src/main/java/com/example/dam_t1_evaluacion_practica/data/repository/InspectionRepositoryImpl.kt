package com.example.dam_t1_evaluacion_practica.data.repository

import com.example.dam_t1_evaluacion_practica.data.local.dao.EvidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.IncidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.InspectionDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.UserDao
import com.example.dam_t1_evaluacion_practica.data.local.entity.EvidenceEntity
import com.example.dam_t1_evaluacion_practica.data.mapper.toDomainModel
import com.example.dam_t1_evaluacion_practica.data.mapper.toEntity
import com.example.dam_t1_evaluacion_practica.domain.error.EvidenceError
import com.example.dam_t1_evaluacion_practica.domain.model.Evidence
import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.model.Incidence
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.domain.model.SyncState
import com.example.dam_t1_evaluacion_practica.domain.model.User
import com.example.dam_t1_evaluacion_practica.domain.repository.InspectionRepository
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-backed implementation of the UI-facing repository contract.
 *
 * Cloud replication is disabled in this slice: every write lands in Room only and every
 * record stays in [SyncState.LOCAL_ONLY]. The sync-related methods keep a local-only
 * behavior so callers can already depend on the full contract.
 */
class InspectionRepositoryImpl(
    private val inspectionDao: InspectionDao,
    private val incidenceDao: IncidenceDao,
    private val userDao: UserDao,
    private val evidenceDao: EvidenceDao
) : InspectionRepository {

    override fun getAllInspections(): Flow<List<InspectionSessionRecord>> {
        return inspectionDao.getAllInspections().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getInspectionsByUser(userEmail: String): Flow<List<InspectionSessionRecord>> {
        return inspectionDao.getInspectionsByUser(userEmail).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getInspectionByFolio(folio: String): Flow<InspectionSessionRecord?> {
        return inspectionDao.getInspectionByFolio(folio).map { entity ->
            entity?.toDomainModel()
        }
    }

    override suspend fun saveInspection(record: InspectionSessionRecord): Long =
        withContext(Dispatchers.IO) {
            inspectionDao.insertInspection(record.toEntity())
        }

    override suspend fun updateInspection(record: InspectionSessionRecord) =
        withContext(Dispatchers.IO) {
            inspectionDao.updateInspection(record.toEntity())
        }

    override fun getAllIncidences(): Flow<List<Incidence>> {
        return incidenceDao.getAllIncidences().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun saveIncidence(incidence: Incidence): Long =
        withContext(Dispatchers.IO) {
            incidenceDao.insertIncidence(incidence.toEntity())
        }

    override suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email)?.toDomainModel()
    }

    override suspend fun login(email: String, password: String): User? =
        withContext(Dispatchers.IO) {
            userDao.login(email, password)?.toDomainModel()
        }

    override suspend fun registerUser(user: User, password: String): Long =
        withContext(Dispatchers.IO) {
            userDao.insertUser(user.toEntity(password))
        }

    override suspend fun deleteInspection(inspectionId: String) = withContext(Dispatchers.IO) {
        // Local tombstone only: no work is enqueued while cloud sync is disabled.
        val entity = inspectionDao.findInspectionById(inspectionId) ?: return@withContext
        inspectionDao.updateInspection(entity.copy(deleted = true, syncState = SyncState.PENDING.name))
    }

    override suspend fun retrySync(inspectionId: String) = withContext(Dispatchers.IO) {
        // Re-queue locally only; the sync worker that consumes PENDING lands later.
        val entity = inspectionDao.findInspectionById(inspectionId) ?: return@withContext
        if (entity.syncState == SyncState.FAILED.name) {
            inspectionDao.updateInspection(entity.copy(syncState = SyncState.PENDING.name))
        }
    }

    override suspend fun registerEvidence(inspectionId: String, itemId: String, ref: String) {
        withContext(Dispatchers.IO) {
            val evidenceId = UUID.nameUUIDFromBytes("$inspectionId|$itemId".toByteArray()).toString()
            evidenceDao.upsert(
                EvidenceEntity(
                    evidenceId = evidenceId,
                    inspectionId = inspectionId,
                    itemId = itemId,
                    localRef = ref,
                    remoteUrl = null,
                    status = EvidenceStatus.PENDING.name,
                    attempts = 0,
                    lastError = null
                )
            )
        }
    }

    override fun observeEvidence(inspectionId: String): Flow<List<Evidence>> {
        return evidenceDao.observeByInspection(inspectionId).map { rows ->
            rows.map { it.toDomainModel() }
        }
    }
}

private fun EvidenceEntity.toDomainModel(): Evidence {
    return Evidence(
        evidenceId = evidenceId,
        inspectionId = inspectionId,
        itemId = itemId,
        localRef = localRef,
        remoteUrl = remoteUrl,
        status = try {
            EvidenceStatus.valueOf(status)
        } catch (e: Exception) {
            EvidenceStatus.PENDING
        },
        attempts = attempts,
        lastError = lastError?.let { name ->
            try {
                EvidenceError.valueOf(name)
            } catch (e: Exception) {
                null
            }
        }
    )
}
