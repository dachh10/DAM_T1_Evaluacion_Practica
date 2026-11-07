package com.example.dam_t1_evaluacion_practica.data.sync

import com.example.dam_t1_evaluacion_practica.data.local.dao.IncidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.InspectionDao
import com.example.dam_t1_evaluacion_practica.data.local.entity.IncidenceEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.InspectionEntity
import com.example.dam_t1_evaluacion_practica.data.remote.firestore.RemoteInspectionDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Room-free test doubles for the SYNC state machine.
 *
 * The design placed a `InMemoryLocalStore` seam in front of the DAOs, but the shipped
 * `SyncCoordinator`/`CloudMirror` read and write the Room DAO interfaces directly. The fakes
 * therefore implement those interfaces instead of inventing a parallel store abstraction.
 */
class FakeInspectionDao : InspectionDao {

    private val rows = linkedMapOf<String, InspectionEntity>()

    fun seed(vararg entities: InspectionEntity) {
        entities.forEach { rows[it.inspectionId] = it }
    }

    fun find(inspectionId: String): InspectionEntity? = rows[inspectionId]

    override fun getAllInspections(): Flow<List<InspectionEntity>> = flowOf(visible())

    override fun getInspectionsByUser(userEmail: String): Flow<List<InspectionEntity>> =
        flowOf(visible().filter { it.userEmail.equals(userEmail, ignoreCase = true) })

    override fun getInspectionByFolio(folio: String): Flow<InspectionEntity?> =
        flowOf(visible().firstOrNull { it.folio == folio })

    override fun getInspectionById(inspectionId: String): Flow<InspectionEntity?> =
        flowOf(rows[inspectionId])

    override fun getInspectionsByOwner(ownerUid: String): Flow<List<InspectionEntity>> =
        flowOf(visible().filter { it.ownerUid == ownerUid })

    override suspend fun findInspectionById(inspectionId: String): InspectionEntity? =
        rows[inspectionId]

    override fun insertInspection(inspection: InspectionEntity): Long {
        rows[inspection.inspectionId] = inspection
        return rows.size.toLong()
    }

    override fun updateInspection(inspection: InspectionEntity) {
        rows[inspection.inspectionId] = inspection
    }

    override fun deleteInspection(inspection: InspectionEntity) {
        rows.remove(inspection.inspectionId)
    }

    private fun visible(): List<InspectionEntity> = rows.values.filter { !it.deleted }
}

/** In-memory `IncidenceDao`. */
class FakeIncidenceDao : IncidenceDao {

    private val rows = linkedMapOf<String, IncidenceEntity>()

    fun seed(vararg entities: IncidenceEntity) {
        entities.forEach { rows[it.id] = it }
    }

    fun all(): List<IncidenceEntity> = rows.values.toList()

    fun find(id: String): IncidenceEntity? = rows[id]

    override fun getAllIncidences(): Flow<List<IncidenceEntity>> =
        flowOf(rows.values.filter { !it.deleted })

    override fun getIncidencesByInspection(inspectionId: String): Flow<List<IncidenceEntity>> =
        flowOf(rows.values.filter { !it.deleted && it.inspectionId == inspectionId })

    override suspend fun findIncidencesByInspection(inspectionId: String): List<IncidenceEntity> =
        rows.values.filter { it.inspectionId == inspectionId }

    override fun insertIncidence(incidence: IncidenceEntity): Long {
        rows[incidence.id] = incidence
        return rows.size.toLong()
    }

    override fun updateIncidence(incidence: IncidenceEntity) {
        rows[incidence.id] = incidence
    }
}

/** Records the inspections handed to the scheduler, without touching WorkManager. */
class RecordingSyncScheduler : SyncScheduler {

    val enqueued = mutableListOf<String>()

    override fun enqueueInspectionSync(inspectionId: String) {
        enqueued += inspectionId
    }
}

/** Records every remote call and returns pre-seeded documents. */
class FakeRemoteDataSource : RemoteInspectionDataSource {

    val calls = mutableListOf<String>()
    val savedInspections = mutableListOf<InspectionEntity>()
    var savedIncidences: Pair<String, List<IncidenceEntity>>? = null
    var deleted: String? = null

    var remoteInspection: InspectionEntity? = null
    var remoteIncidences: List<IncidenceEntity> = emptyList()
    var failWith: Exception? = null

    override suspend fun saveInspection(entity: InspectionEntity) {
        failWith?.let { throw it }
        calls += "saveInspection"
        savedInspections += entity
    }

    override suspend fun saveIncidences(inspectionId: String, entities: List<IncidenceEntity>) {
        failWith?.let { throw it }
        calls += "saveIncidences"
        savedIncidences = inspectionId to entities
    }

    override suspend fun deleteInspection(inspectionId: String) {
        failWith?.let { throw it }
        calls += "deleteInspection"
        deleted = inspectionId
    }

    override suspend fun getInspection(inspectionId: String): InspectionEntity? {
        failWith?.let { throw it }
        calls += "getInspection"
        return remoteInspection
    }

    override suspend fun getIncidences(inspectionId: String): List<IncidenceEntity> {
        failWith?.let { throw it }
        calls += "getIncidences"
        return remoteIncidences
    }
}
