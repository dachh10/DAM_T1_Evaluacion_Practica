package com.example.dam_t1_evaluacion_practica.domain.model

/**
 * A persisted inspection result. Mirrors the [Incidence] domain pattern.
 *
 * @param scheduledReinspectionAt nullable epoch millis when a reinspection is due;
 *        null means the inspection was compliant (score >= 85) and needs no reinspection.
 */
data class Inspection(
    val id: String,
    val moduleCode: String,
    val moduleTitle: String,
    val score: Int,
    val criticalStopTriggered: Boolean,
    val scheduledReinspectionAt: Long?,
    val createdAt: Long
)
