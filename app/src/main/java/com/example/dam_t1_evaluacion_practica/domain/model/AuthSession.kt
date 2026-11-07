package com.example.dam_t1_evaluacion_practica.domain.model

/**
 * Authenticated identity established by the auth data source.
 * The uid scopes every cloud document owned by the inspector.
 */
data class AuthSession(
    val uid: String,
    val email: String
)
