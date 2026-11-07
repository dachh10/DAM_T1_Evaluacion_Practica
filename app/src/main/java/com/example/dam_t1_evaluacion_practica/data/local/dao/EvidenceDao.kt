package com.example.dam_t1_evaluacion_practica.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dam_t1_evaluacion_practica.data.local.entity.EvidenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence WHERE inspectionId = :inspectionId ORDER BY itemId ASC")
    fun observeByInspection(inspectionId: String): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence WHERE inspectionId = :inspectionId ORDER BY itemId ASC")
    suspend fun getByInspection(inspectionId: String): List<EvidenceEntity>

    @Query("SELECT * FROM evidence WHERE evidenceId = :evidenceId LIMIT 1")
    suspend fun getById(evidenceId: String): EvidenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(evidence: EvidenceEntity)
}
