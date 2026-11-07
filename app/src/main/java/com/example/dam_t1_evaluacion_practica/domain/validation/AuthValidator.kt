package com.example.dam_t1_evaluacion_practica.domain.validation

import com.example.dam_t1_evaluacion_practica.domain.error.AuthError
import com.example.dam_t1_evaluacion_practica.domain.model.SyncState
import com.example.dam_t1_evaluacion_practica.domain.model.User

/**
 * Pure validation rules applied before any network request (design section 2).
 *
 * Email format is enforced for both login and registration. Password length is enforced
 * for registration only: design section 2 keeps `login("test@seguridad.mx", "pass")`
 * valid because Firebase remains the authority for sign-in credentials.
 */
object AuthValidator {

    /** Minimum password length accepted at registration. */
    const val MIN_PASSWORD_LENGTH = 6

    private val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Returns true when [email] looks like a deliverable address:
     * a local part, an `@`, a domain and a dot-separated suffix.
     */
    fun isValidEmail(email: String): Boolean = EMAIL_PATTERN.matches(email)

    /**
     * Validates a sign-in attempt. Password length is intentionally not checked here.
     *
     * @return null when the input may be sent, otherwise the domain error to surface.
     */
    fun validateLogin(email: String, password: String): AuthError? {
        return validateEmail(email)
    }

    /**
     * Validates a registration attempt, which additionally requires a password of at
     * least [MIN_PASSWORD_LENGTH] characters.
     *
     * @return null when the input may be sent, otherwise the domain error to surface.
     */
    fun validateRegistration(email: String, password: String): AuthError? {
        val emailError = validateEmail(email)
        if (emailError != null) return emailError
        if (password.length < MIN_PASSWORD_LENGTH) return AuthError.WEAK_PASSWORD
        return null
    }

    private fun validateEmail(email: String): AuthError? {
        return if (isValidEmail(email)) null else AuthError.INVALID_CREDENTIALS
    }
}

/**
 * Conflict rule for cloud mirroring (design section 6).
 *
 * A remote snapshot is applied locally **only if** the local record is `SYNCED` and the
 * remote one is newer. A local record in `LOCAL_ONLY`, `PENDING` or `FAILED` ignores
 * remote snapshots entirely — the dirty local copy wins until its push is acknowledged.
 * A remote `REMOVED` deletes locally only when the local record is `SYNCED`.
 */
object SyncConflictPolicy {

    /**
     * Returns `true` when a remote snapshot should overwrite the local record.
     */
    fun shouldApplyRemote(
        localSyncState: SyncState,
        localUpdatedAt: Long?,
        remoteUpdatedAt: Long?
    ): Boolean {
        if (localSyncState != SyncState.SYNCED) return false
        if (remoteUpdatedAt == null) return false
        if (localUpdatedAt == null) return true
        return remoteUpdatedAt > localUpdatedAt
    }

    /**
     * Returns `true` when a remote `REMOVED` change should delete the local record.
     */
    fun shouldDeleteOnRemoved(localSyncState: SyncState): Boolean =
        localSyncState == SyncState.SYNCED
}
