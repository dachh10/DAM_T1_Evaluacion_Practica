package com.example.dam_t1_evaluacion_practica.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Durable upload queue row for one evidence asset.
 *
 * [localRef] is the device-local file or content reference; [remoteUrl] stays null until
 * the upload succeeds and becomes the reference of record.
 */
@Entity(tableName = "evidence")
data class EvidenceEntity(
    @PrimaryKey val evidenceId: String,
    val inspectionId: String,
    val itemId: String,
    val localRef: String,
    val remoteUrl: String?,
    val status: String,
    val attempts: Int,
    val lastError: String?
)
