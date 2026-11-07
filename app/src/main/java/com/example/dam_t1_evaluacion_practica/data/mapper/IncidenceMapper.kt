package com.example.dam_t1_evaluacion_practica.data.mapper

import com.example.dam_t1_evaluacion_practica.data.local.entity.IncidenceEntity
import com.example.dam_t1_evaluacion_practica.domain.model.Incidence
import com.example.dam_t1_evaluacion_practica.domain.model.IncidenceStatus

fun IncidenceEntity.toDomainModel(): Incidence {
    return Incidence(
        id = id,
        inspectionId = inspectionId,
        checklistItemId = checklistItemId,
        status = try {
            IncidenceStatus.valueOf(status)
        } catch (e: Exception) {
            IncidenceStatus.OPEN
        },
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        photoUri = photoUri,
        notes = notes
    )
}

fun Incidence.toEntity(): IncidenceEntity {
    return IncidenceEntity(
        id = id,
        inspectionId = inspectionId,
        checklistItemId = checklistItemId,
        status = status.name,
        timestamp = timestamp,
        updatedAt = timestamp,
        latitude = latitude,
        longitude = longitude,
        photoUri = photoUri,
        notes = notes
    )
}
