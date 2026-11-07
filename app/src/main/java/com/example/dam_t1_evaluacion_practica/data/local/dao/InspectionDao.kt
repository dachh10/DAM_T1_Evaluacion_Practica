package com.example.dam_t1_evaluacion_practica.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dam_t1_evaluacion_practica.data.local.entity.InspectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {

    @Query("SELECT * FROM inspections WHERE deleted = 0 ORDER BY timestamp DESC")
    fun getAllInspections(): Flow<List<InspectionEntity>>

    @Query(
        "SELECT * FROM inspections WHERE deleted = 0 AND LOWER(userEmail) = LOWER(:userEmail) " +
            "ORDER BY timestamp DESC"
    )
    fun getInspectionsByUser(userEmail: String): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE deleted = 0 AND folio = :folio LIMIT 1")
    fun getInspectionByFolio(folio: String): Flow<InspectionEntity?>

    @Query("SELECT * FROM inspections WHERE deleted = 0 AND inspectionId = :inspectionId LIMIT 1")
    fun getInspectionById(inspectionId: String): Flow<InspectionEntity?>

    @Query(
        "SELECT * FROM inspections WHERE deleted = 0 AND ownerUid = :ownerUid " +
            "ORDER BY updatedAt DESC"
    )
    fun getInspectionsByOwner(ownerUid: String): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE inspectionId = :inspectionId LIMIT 1")
    suspend fun findInspectionById(inspectionId: String): InspectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertInspection(inspection: InspectionEntity): Long

    @Update
    fun updateInspection(inspection: InspectionEntity)

    @Delete
    fun deleteInspection(inspection: InspectionEntity)
}
