package com.example.dam_t1_evaluacion_practica.data.remote.auth

import com.example.dam_t1_evaluacion_practica.domain.error.AuthError
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the Firebase `errorCode` -> [AuthError] mapping table from design section 9.
 *
 * The mapper receives only the raw code string so it stays a pure JVM function; the
 * Firebase exception type never leaves [FirebaseAuthDataSource].
 */
class AuthErrorMapperTest {

    @Test
    fun `invalid credential codes map to invalid credentials`() {
        assertEquals(
            AuthError.INVALID_CREDENTIALS,
            AuthErrorMapper.fromErrorCode("ERROR_INVALID_CREDENTIALS")
        )
        assertEquals(
            AuthError.INVALID_CREDENTIALS,
            AuthErrorMapper.fromErrorCode("ERROR_WRONG_PASSWORD")
        )
        assertEquals(
            AuthError.INVALID_CREDENTIALS,
            AuthErrorMapper.fromErrorCode("ERROR_USER_NOT_FOUND")
        )
        assertEquals(
            AuthError.INVALID_CREDENTIALS,
            AuthErrorMapper.fromErrorCode("ERROR_INVALID_EMAIL")
        )
    }

    @Test
    fun `email already in use code maps to email already in use`() {
        assertEquals(
            AuthError.EMAIL_ALREADY_IN_USE,
            AuthErrorMapper.fromErrorCode("ERROR_EMAIL_ALREADY_IN_USE")
        )
    }

    @Test
    fun `weak password code maps to weak password`() {
        assertEquals(
            AuthError.WEAK_PASSWORD,
            AuthErrorMapper.fromErrorCode("ERROR_WEAK_PASSWORD")
        )
    }

    @Test
    fun `network request failed code maps to network unavailable`() {
        assertEquals(
            AuthError.NETWORK_UNAVAILABLE,
            AuthErrorMapper.fromErrorCode("ERROR_NETWORK_REQUEST_FAILED")
        )
    }

    @Test
    fun `too many requests code maps to unknown`() {
        assertEquals(
            AuthError.UNKNOWN,
            AuthErrorMapper.fromErrorCode("ERROR_TOO_MANY_REQUESTS")
        )
    }

    @Test
    fun `unmapped code maps to unknown`() {
        assertEquals(
            AuthError.UNKNOWN,
            AuthErrorMapper.fromErrorCode("ERROR_OPERATION_NOT_ALLOWED")
        )
    }

    @Test
    fun `null or blank code maps to unknown`() {
        assertEquals(AuthError.UNKNOWN, AuthErrorMapper.fromErrorCode(null))
        assertEquals(AuthError.UNKNOWN, AuthErrorMapper.fromErrorCode(""))
    }
}
