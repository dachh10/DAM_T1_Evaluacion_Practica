package com.example.dam_t1_evaluacion_practica.data.sync

import com.example.dam_t1_evaluacion_practica.data.local.dao.IncidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.InspectionDao
import com.example.dam_t1_evaluacion_practica.data.local.entity.IncidenceEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.InspectionEntity
import com.example.dam_t1_evaluacion_practica.data.remote.firestore.RemoteInspectionDataSource
import com.example.dam_t1_evaluacion_practica.domain.model.SyncState
import com.example.dam_t1_evaluacion_practica.domain.validation.SyncConflictPolicy
import kotlin.collections.associateBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CloudMirror(
    private val inspectionDao: InspectionDao,
    private val incidenceDao: IncidenceDao,
    private val remote: RemoteInspectionDataSource,
    private val policy: SyncConflictPolicy
) {

    suspend fun mirrorInspection(inspectionId: String) = withContext(Dispatchers.IO) {
        val remoteInspection = remote.getInspection(inspectionId) ?: return@withContext
        val localInspection = inspectionDao.findInspectionById(inspectionId)

        if (localInspection == null) {
            inspectionDao.insertInspection(remoteInspection.copy(syncState = SyncState.SYNCED.name))
        } else if (policy.shouldApplyRemote(
            syncStateOf(localInspection),
            localInspection.lastSyncedAt,
            remoteInspection.lastSyncedAt
        )) {
            inspectionDao.updateInspection(remoteInspection.copy(syncState = SyncState.SYNCED.name))
        }

        val remoteIncidences = remote.getIncidences(inspectionId)
        val localIncidences = incidenceDao.findIncidencesByInspection(inspectionId)
        val localById = localIncidences.associateBy { it.id }

        remoteIncidences.forEach { remoteInc ->
            val localInc = localById[remoteInc.id]
            if (localInc == null) {
                incidenceDao.insertIncidence(remoteInc.copy(syncState = SyncState.SYNCED.name))
            } else if (policy.shouldApplyRemote(
                syncStateOf(localInc),
                localInc.updatedAt,
                remoteInc.updatedAt
            )) {
                incidenceDao.updateInspection(remoteInc.copy(syncState = SyncState.SYNCED.name))
            }
        }
    }

    suspend fun handleRemoved(inspectionId: String) = withContext(Dispatchers.IO) {
        val local = inspectionDao.findInspectionById(inspectionId)
        if (local != null && policy.shouldDeleteOnRemoved(syncStateOf(local))) {
            inspectionDao.updateInspection(local.copy(deleted = true, syncState = SyncState.PENDING.name))
            return@withContext
        }
    }

    private fun syncStateOf(entity: InspectionEntity): SyncState =
        SyncState.valueOf(entity.syncState)

    private fun syncStateOf(entity: IncidenceEntity): SyncState =
        SyncState.valueOf(entity.syncState)
}
