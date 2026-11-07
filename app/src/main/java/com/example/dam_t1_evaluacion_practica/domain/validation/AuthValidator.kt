package com.example.dam_t1_evaluacion_practica.domain.validation

import com.example.dam_t1_evaluacion_practica.domain.error.AuthError

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
