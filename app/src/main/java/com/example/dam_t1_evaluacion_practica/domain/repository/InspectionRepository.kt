package com.example.dam_t1_evaluacion_practica.domain.repository

import com.example.dam_t1_evaluacion_practica.domain.model.Evidence
import com.example.dam_t1_evaluacion_practica.domain.model.Incidence
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * UI-facing contract for inspection persistence.
 *
 * Room remains the read path; the implementation is backed by the local store and
 * mirrors writes to the cloud. The ViewModel consumes this interface with a single
 * constructor parameter, which also exposes the evidence subset it needs.
 */
interface InspectionRepository {

    fun getAllInspections(): Flow<List<InspectionSessionRecord>>

    fun getInspectionsByUser(userEmail: String): Flow<List<InspectionSessionRecord>>

    fun getInspectionByFolio(folio: String): Flow<InspectionSessionRecord?>

    suspend fun saveInspection(record: InspectionSessionRecord): Long

    suspend fun updateInspection(record: InspectionSessionRecord)

    fun getAllIncidences(): Flow<List<Incidence>>

    suspend fun saveIncidence(incidence: Incidence): Long

    suspend fun getUserByEmail(email: String): User?

    suspend fun login(email: String, password: String): User?

    suspend fun registerUser(user: User, password: String): User

    /** Marks the inspection as a local tombstone and enqueues its remote deletion. */
    suspend fun deleteInspection(inspectionId: String)

    /** Requeues a FAILED inspection for another sync attempt. */
    suspend fun retrySync(inspectionId: String)

    /** Registers evidence for an inspection item and enqueues its upload. */
    suspend fun registerEvidence(inspectionId: String, itemId: String, ref: String)

    fun observeEvidence(inspectionId: String): Flow<List<Evidence>>
}
