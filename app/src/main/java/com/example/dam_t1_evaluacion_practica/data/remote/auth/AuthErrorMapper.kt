package com.example.dam_t1_evaluacion_practica.data.remote.auth

import com.example.dam_t1_evaluacion_practica.domain.error.AuthError

/**
 * Pure translation of Firebase Auth `errorCode` values into [AuthError].
 *
 * Keeping the mapping on the plain string is what makes this table unit-testable on the
 * JVM: `FirebaseAuthException` cannot be constructed outside the SDK, so the data source
 * extracts the code at its boundary and passes it here.
 */
object AuthErrorMapper {

    /**
     * Maps a raw Firebase Auth error code (design section 9 table).
     * Unknown, null or blank codes fall back to [AuthError.UNKNOWN].
     */
    fun fromErrorCode(errorCode: String?): AuthError = when (errorCode) {
        "ERROR_INVALID_CREDENTIALS",
        "ERROR_WRONG_PASSWORD",
        "ERROR_USER_NOT_FOUND",
        "ERROR_INVALID_EMAIL" -> AuthError.INVALID_CREDENTIALS

        "ERROR_EMAIL_ALREADY_IN_USE" -> AuthError.EMAIL_ALREADY_IN_USE

        "ERROR_WEAK_PASSWORD" -> AuthError.WEAK_PASSWORD

        "ERROR_NETWORK_REQUEST_FAILED" -> AuthError.NETWORK_UNAVAILABLE

        else -> AuthError.UNKNOWN
    }
}
