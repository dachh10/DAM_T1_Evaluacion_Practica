package com.example.dam_t1_evaluacion_practica.data.mapper

import com.example.dam_t1_evaluacion_practica.data.local.entity.EvidenceEntity
import com.example.dam_t1_evaluacion_practica.domain.model.Evidence
import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.error.EvidenceError

fun EvidenceEntity.toDomainModel(): Evidence {
    return Evidence(
        evidenceId = evidenceId,
        inspectionId = inspectionId,
        itemId = itemId,
        localRef = localRef,
        remoteUrl = remoteUrl,
        status = try {
            EvidenceStatus.valueOf(status)
        } catch (e: Exception) {
            EvidenceStatus.PENDING
        },
        attempts = attempts,
        lastError = lastError?.let { name ->
            try {
                EvidenceError.valueOf(name)
            } catch (e: Exception) {
                null
            }
        }
    )
}

fun Evidence.toEntity(): EvidenceEntity {
    return EvidenceEntity(
        evidenceId = evidenceId,
        inspectionId = inspectionId,
        itemId = itemId,
        localRef = localRef,
        remoteUrl = remoteUrl,
        status = status.name,
        attempts = attempts,
        lastError = lastError?.name
    )
}