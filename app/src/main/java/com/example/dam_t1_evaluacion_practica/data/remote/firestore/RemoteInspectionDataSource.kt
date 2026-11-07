package com.example.dam_t1_evaluacion_practica.data.remote.firestore

import com.example.dam_t1_evaluacion_practica.data.local.entity.IncidenceEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.InspectionEntity

/**
 * Remote persistence of inspections and incidences against Firestore (design section 6).
 *
 * The data source works with Room entities directly: entities already carry every
 * column the cloud document needs, and the remote mappers ([InspectionRemoteMapper],
 * [IncidenceRemoteMapper]) convert them to plain Maps for Firestore.
 */
interface RemoteInspectionDataSource {

    /** Writes (or overwrites) a single inspection document. */
    suspend fun saveInspection(entity: InspectionEntity)

    /** Batch-writes all incidences of one inspection under its document. */
    suspend fun saveIncidences(inspectionId: String, entities: List<IncidenceEntity>)

    /** Deletes the inspection document and all its incidences sub-documents. */
    suspend fun deleteInspection(inspectionId: String)

    /** Reads a single inspection document, or null if it does not exist. */
    suspend fun getInspection(inspectionId: String): InspectionEntity?

    /** Reads all incidences of one inspection. */
    suspend fun getIncidences(inspectionId: String): List<IncidenceEntity>
}
