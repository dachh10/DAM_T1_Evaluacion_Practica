package com.example.dam_t1_evaluacion_practica.domain.model

data class Normativ(
    val code: String,
    val title: String = "",
    val description: String = "",
    val officialUrl: String? = null,
    val keyRequirement: String? = null,
    val issuingAuthority: String = "STPS"
)
