package com.example.dam_t1_evaluacion_practica.domain.error

/**
 * Carrier for a mapped [AuthError]. Sign-in and registration keep their declared
 * return types; failures are surfaced by throwing this exception.
 */
class AuthException(val error: AuthError) : Exception(error.name)
