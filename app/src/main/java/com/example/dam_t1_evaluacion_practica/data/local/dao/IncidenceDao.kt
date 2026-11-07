package com.example.dam_t1_evaluacion_practica.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dam_t1_evaluacion_practica.data.local.entity.IncidenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidenceDao {

    @Query("SELECT * FROM incidences WHERE deleted = 0 ORDER BY timestamp DESC")
    fun getAllIncidences(): Flow<List<IncidenceEntity>>

    @Query(
        "SELECT * FROM incidences WHERE deleted = 0 AND inspectionId = :inspectionId " +
            "ORDER BY timestamp DESC"
    )
    fun getIncidencesByInspection(inspectionId: String): Flow<List<IncidenceEntity>>

    @Query(
        "SELECT * FROM incidences WHERE deleted = 0 AND inspectionId = :inspectionId " +
            "ORDER BY timestamp DESC"
    )
    suspend fun findIncidencesByInspection(inspectionId: String): List<IncidenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertIncidence(incidence: IncidenceEntity): Long
}
