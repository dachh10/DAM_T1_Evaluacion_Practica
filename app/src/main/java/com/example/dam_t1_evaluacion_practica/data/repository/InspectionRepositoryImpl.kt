package com.example.dam_t1_evaluacion_practica.data.repository

import com.example.dam_t1_evaluacion_practica.data.local.dao.EvidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.IncidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.InspectionDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.UserDao
import com.example.dam_t1_evaluacion_practica.data.local.entity.EvidenceEntity
import com.example.dam_t1_evaluacion_practica.data.mapper.toDomainModel
import com.example.dam_t1_evaluacion_practica.data.mapper.toEntity
import com.example.dam_t1_evaluacion_practica.data.remote.firestore.RemoteInspectionDataSource
import com.example.dam_t1_evaluacion_practica.domain.error.EvidenceError
import com.example.dam_t1_evaluacion_practica.domain.model.Evidence
import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceId
import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.model.Incidence
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.domain.model.SyncState
import com.example.dam_t1_evaluacion_practica.domain.model.User
import com.example.dam_t1_evaluacion_practica.data.sync.SyncCoordinator
import com.example.dam_t1_evaluacion_practica.domain.repository.EvidenceRepository
import com.example.dam_t1_evaluacion_practica.domain.repository.InspectionRepository
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class InspectionRepositoryImpl(
    private val inspectionDao: InspectionDao,
    private val incidenceDao: IncidenceDao,
    private val userDao: UserDao,
    private val evidenceDao: EvidenceDao,
    private val remote: RemoteInspectionDataSource,
    private val coordinator: SyncCoordinator,
    private val evidenceRepository: EvidenceRepository
) : InspectionRepository {

    override fun getAllInspections(): Flow<List<InspectionSessionRecord>> =
        inspectionDao.getAllInspections().map { entities ->
            entities.map { it.toDomainModel() }
        }

    override fun getInspectionsByUser(userEmail: String): Flow<List<InspectionSessionRecord>> =
        inspectionDao.getInspectionsByUser(userEmail).map { entities ->
            entities.map { it.toDomainModel() }
        }

    override fun getInspectionByFolio(folio: String): Flow<InspectionSessionRecord?> =
        inspectionDao.getInspectionByFolio(folio).map { it?.toDomainModel() }

    override suspend fun saveInspection(record: InspectionSessionRecord): Long =
        withContext(Dispatchers.IO) {
            val id = inspectionDao.insertInspection(record.toEntity())
            // Mark all incidences of this inspection as PENDING so the worker pushes them
            val incidences = incidenceDao.findIncidencesByInspection(record.inspectionId)
            incidences.forEach { inc ->
                incidenceDao.updateIncidence(inc.copy(syncState = SyncState.PENDING.name))
            }
            coordinator.markPending(record.inspectionId)
            id
        }

    override suspend fun updateInspection(record: InspectionSessionRecord) =
        withContext(Dispatchers.IO) {
            inspectionDao.updateInspection(record.toEntity())
            // Mark all incidences of this inspection as PENDING so the worker pushes them
            val incidences = incidenceDao.findIncidencesByInspection(record.inspectionId)
            incidences.forEach { inc ->
                incidenceDao.updateIncidence(inc.copy(syncState = SyncState.PENDING.name))
            }
            coordinator.markPending(record.inspectionId)
        }

    override fun getAllIncidences(): Flow<List<Incidence>> =
        incidenceDao.getAllIncidences().map { entities ->
            entities.map { it.toDomainModel() }
        }

    override suspend fun saveIncidence(incidence: Incidence): Long =
        withContext(Dispatchers.IO) {
            val entity = incidence.toEntity().copy(syncState = SyncState.PENDING.name)
            incidenceDao.insertIncidence(entity)
        }

    override suspend fun getUserByEmail(email: String): User? =
        withContext(Dispatchers.IO) {
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
        val entity = inspectionDao.findInspectionById(inspectionId) ?: return@withContext
        remote.deleteInspection(inspectionId)
        inspectionDao.updateInspection(entity.copy(deleted = true, syncState = SyncState.PENDING.name))
    }

    override suspend fun retrySync(inspectionId: String) = withContext(Dispatchers.IO) {
        val entity = inspectionDao.findInspectionById(inspectionId) ?: return@withContext
        if (entity.syncState == SyncState.FAILED.name) {
            inspectionDao.updateInspection(entity.copy(syncState = SyncState.PENDING.name))
            coordinator.syncInspection(inspectionId)
        }
    }

    override suspend fun registerEvidence(inspectionId: String, itemId: String, ref: String) {
        evidenceRepository.register(inspectionId, itemId, ref)
    }

    override fun observeEvidence(inspectionId: String): Flow<List<Evidence>> =
        evidenceRepository.observe(inspectionId)
}