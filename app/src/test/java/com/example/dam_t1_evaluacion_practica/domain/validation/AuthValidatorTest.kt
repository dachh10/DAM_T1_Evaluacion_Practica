package com.example.dam_t1_evaluacion_practica.domain.validation

import com.example.dam_t1_evaluacion_practica.domain.error.AuthError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract for the pure auth validation rules (design section 2).
 *
 * Email format is validated on every entry point. Password length is a registration-only
 * rule: design section 2 keeps `login("test@seguridad.mx", "pass")` valid because Firebase
 * is the authority for sign-in credentials.
 */
class AuthValidatorTest {

    // --- Email format ---

    @Test
    fun `email without at sign is rejected`() {
        assertFalse(AuthValidator.isValidEmail("correo"))
    }

    @Test
    fun `email with local part, domain and dot suffix is accepted`() {
        assertTrue(AuthValidator.isValidEmail("test@seguridad.mx"))
    }

    @Test
    fun `email containing spaces is rejected`() {
        assertFalse(AuthValidator.isValidEmail("test @seguridad.mx"))
    }

    @Test
    fun `login with malformed email maps to invalid credentials`() {
        assertEquals(
            AuthError.INVALID_CREDENTIALS,
            AuthValidator.validateLogin("correo", "password")
        )
    }

    @Test
    fun `registration with malformed email maps to invalid credentials`() {
        assertEquals(
            AuthError.INVALID_CREDENTIALS,
            AuthValidator.validateRegistration("correo", "password")
        )
    }

    // --- Password boundary: registration requires at least 6 characters ---

    @Test
    fun `registration with five character password is weak`() {
        assertEquals(
            AuthError.WEAK_PASSWORD,
            AuthValidator.validateRegistration("test@seguridad.mx", "12345")
        )
    }

    @Test
    fun `registration with six character password is accepted`() {
        assertNull(AuthValidator.validateRegistration("test@seguridad.mx", "123456"))
    }

    @Test
    fun `registration with empty password is weak`() {
        assertEquals(
            AuthError.WEAK_PASSWORD,
            AuthValidator.validateRegistration("test@seguridad.mx", "")
        )
    }

    // --- Register-vs-login asymmetry: length is registration-only ---

    @Test
    fun `login does not enforce password length`() {
        assertNull(AuthValidator.validateLogin("test@seguridad.mx", "pass"))
    }

    @Test
    fun `same short password passes login but fails registration`() {
        val email = "test@seguridad.mx"
        val shortPassword = "12345"

        assertNull(AuthValidator.validateLogin(email, shortPassword))
        assertEquals(
            AuthError.WEAK_PASSWORD,
            AuthValidator.validateRegistration(email, shortPassword)
        )
    }

    @Test
    fun `valid login and registration both return no error`() {
        val email = "test@seguridad.mx"
        val password = "123456"

        assertNull(AuthValidator.validateLogin(email, password))
        assertNull(AuthValidator.validateRegistration(email, password))
    }
}
