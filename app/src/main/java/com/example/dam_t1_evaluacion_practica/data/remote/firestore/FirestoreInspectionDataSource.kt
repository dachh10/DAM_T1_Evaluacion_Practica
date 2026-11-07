package com.example.dam_t1_evaluacion_practica.data.remote.firestore

import com.example.dam_t1_evaluacion_practica.data.local.entity.IncidenceEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.InspectionEntity
import com.example.dam_t1_evaluacion_practica.data.mapper.toDomainModel
import com.example.dam_t1_evaluacion_practica.data.mapper.toEntity
import com.example.dam_t1_evaluacion_practica.data.remote.mapper.IncidenceRemoteMapper
import com.example.dam_t1_evaluacion_practica.data.remote.mapper.InspectionRemoteMapper
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirestoreInspectionDataSource internal constructor(
    private val firestore: FirebaseFirestore
) : RemoteInspectionDataSource {
    constructor() : this(FirebaseFirestore.getInstance())

    override suspend fun saveInspection(entity: InspectionEntity) {
        withContext(Dispatchers.IO) {
            val map = InspectionRemoteMapper.toMap(entity.toDomainModel()).toMutableMap()
            map["updatedAt"] = FieldValue.serverTimestamp()
            Tasks.await(
                firestore.collection("inspections").document(entity.inspectionId)
                    .set(map)
            )
        }
    }

    override suspend fun saveIncidences(inspectionId: String, entities: List<IncidenceEntity>) {
        withContext(Dispatchers.IO) {
            val incidencesRef = firestore.collection("inspections")
                .document(inspectionId)
                .collection("incidences")
            val batch = firestore.batch()
            entities.forEach { entity ->
                val map = IncidenceRemoteMapper.toMap(entity.toDomainModel()).toMutableMap()
                map["updatedAt"] = FieldValue.serverTimestamp()
                batch.set(incidencesRef.document(entity.id), map)
            }
            Tasks.await(batch.commit())
        }
    }

    override suspend fun deleteInspection(inspectionId: String) {
        withContext(Dispatchers.IO) {
            val incidences = getIncidences(inspectionId)
            val batch = firestore.batch()
            incidences.forEach {
                batch.delete(firestore.collection("inspections")
                    .document(inspectionId)
                    .collection("incidences")
                    .document(it.id))
            }
            batch.delete(firestore.collection("inspections").document(inspectionId))
            Tasks.await(batch.commit())
        }
    }

    override suspend fun getInspection(inspectionId: String): InspectionEntity? {
        return withContext(Dispatchers.IO) {
            val doc = Tasks.await(
                firestore.collection("inspections").document(inspectionId).get()
            )
            if (!doc.exists()) null else InspectionRemoteMapper.fromMap(inspectionId, doc.data!!).toEntity()
        }
    }

    override suspend fun getIncidences(inspectionId: String): List<IncidenceEntity> {
        return withContext(Dispatchers.IO) {
            val snapshot = Tasks.await(
                firestore.collection("inspections")
                    .document(inspectionId)
                    .collection("incidences")
                    .get()
            )
            snapshot.documents.mapNotNull { doc ->
                IncidenceRemoteMapper.fromMap(doc.id, doc.data!!).toEntity()
            }
        }
    }
}
