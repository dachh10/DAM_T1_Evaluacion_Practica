package com.example.dam_t1_evaluacion_practica.domain.model

data class User(
    val email: String,
    val name: String,
    val cedula: String,
    val createdAt: Long = System.currentTimeMillis()
)
