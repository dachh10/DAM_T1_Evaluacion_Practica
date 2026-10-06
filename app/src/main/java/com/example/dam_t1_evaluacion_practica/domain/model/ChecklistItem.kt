package com.example.dam_t1_evaluacion_practica.domain.model

enum class CriticalityLevel(val points: Int) {
    HIGH(5),
    MEDIUM(3),
    LOW(1)
}

data class ChecklistItem(
    val id: String,
    val description: String,
    val normativ: Normativ,
    val criticalityLevel: CriticalityLevel,
    val requiresPhotoEvidence: Boolean,
    val secondaryNorms: List<Normativ> = emptyList()
) {
    val allNorms: List<Normativ>
        get() = listOf(normativ) + secondaryNorms
}
