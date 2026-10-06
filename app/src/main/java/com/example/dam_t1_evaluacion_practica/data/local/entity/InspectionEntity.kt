package com.example.dam_t1_evaluacion_practica.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inspections")
data class InspectionEntity(
    @PrimaryKey val folio: String,
    val userEmail: String = "",
    val companyName: String,
    val zoneName: String,
    val auditType: String,
    val inspectorName: String,
    val inspectorCedula: String,
    val timestamp: Long,
    val overallScore: Int,
    val criticalStop: Boolean,
    val reinspectionDue: Boolean,
    val scheduledReinspectionAt: Long?,
    val status: String,
    val latitude: Double?,
    val longitude: Double?,
    val modulesJson: String,
    val findingsJson: String,
    val normCodesJson: String
)
