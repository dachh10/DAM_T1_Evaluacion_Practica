package com.example.dam_t1_evaluacion_practica.domain.repository

import com.example.dam_t1_evaluacion_practica.domain.model.Evidence
import kotlinx.coroutines.flow.Flow

/**
 * Worker-facing evidence contract. The inspection repository delegates its evidence
 * methods here so the ViewModel keeps a single constructor parameter.
 */
interface EvidenceRepository {

    fun observe(inspectionId: String): Flow<List<Evidence>>

    suspend fun register(inspectionId: String, itemId: String, localRef: String): Evidence

    suspend fun retry(evidenceId: String)
}
