package com.example.dam_t1_evaluacion_practica.domain.error

/**
 * Authentication failures exposed by the domain layer.
 * Raw Firebase error codes never leave the data layer.
 */
enum class AuthError {
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_IN_USE,
    WEAK_PASSWORD,
    NETWORK_UNAVAILABLE,
    UNKNOWN
}
