package com.example.dam_t1_evaluacion_practica.domain.model

enum class IncidenceStatus {
    OPEN, IN_MITIGATION, CLOSED
}

data class Incidence(
    val id: String,
    // Parent inspection id; the ViewModel stamps it at creation. The default keeps
    // the pre-existing construction sites compiling until that change lands.
    val inspectionId: String = "",
    val checklistItemId: String,
    val status: IncidenceStatus,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val photoUri: String?, // URI to device gallery
    val notes: String?
)
