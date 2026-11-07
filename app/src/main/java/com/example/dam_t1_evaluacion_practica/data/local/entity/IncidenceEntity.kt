package com.example.dam_t1_evaluacion_practica.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidences")
data class IncidenceEntity(
    @PrimaryKey val id: String,
    val inspectionId: String,
    val checklistItemId: String,
    val status: String,
    val timestamp: Long,
    val updatedAt: Long,
    val latitude: Double?,
    val longitude: Double?,
    val photoUri: String?,
    val notes: String?,
    val syncState: String = "LOCAL_ONLY",
    val deleted: Boolean = false
)
