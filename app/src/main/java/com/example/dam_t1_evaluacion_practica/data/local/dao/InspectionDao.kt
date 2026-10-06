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
    @Query("SELECT * FROM inspections ORDER BY timestamp DESC")
    fun getAllInspections(): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE LOWER(userEmail) = LOWER(:userEmail) ORDER BY timestamp DESC")
    fun getInspectionsByUser(userEmail: String): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE folio = :folio LIMIT 1")
    fun getInspectionByFolio(folio: String): Flow<InspectionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertInspection(inspection: InspectionEntity): Long

    @Update
    fun updateInspection(inspection: InspectionEntity)

    @Delete
    fun deleteInspection(inspection: InspectionEntity)
}
