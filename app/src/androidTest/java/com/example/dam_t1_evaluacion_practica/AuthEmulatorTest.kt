package com.example.dam_t1_evaluacion_practica

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for Firebase Auth against the Auth emulator.
 *
 * Requires the emulator to be running:
 *   firebase emulators:start --only auth,firestore
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AuthEmulatorTest {

    @ClassRule
    val emulatorRule = FirebaseEmulatorRule()

    private val auth = FirebaseAuth.getInstance()

    @Test
    fun `register new user succeeds`() = runTest {
        val email = "test-${System.currentTimeMillis()}@seguridad.mx"
        val password = "password123"

        val result = Tasks.await(auth.createUserWithEmailAndPassword(email, password))
        assertNotNull(result.user)
        assertEquals(email, result.user?.email)
    }

    @Test
    fun `duplicate email throws EMAIL_ALREADY_IN_USE`() = runTest {
        val email = "duplicate-${System.currentTimeMillis()}@seguridad.mx"
        val password = "password123"

        Tasks.await(auth.createUserWithEmailAndPassword(email, password))

        try {
            Tasks.await(auth.createUserWithEmailAndPassword(email, password))
            assertTrue("Should have thrown FirebaseAuthException", false)
        } catch (e: FirebaseAuthException) {
            assertEquals("EMAIL_ALREADY_IN_USE", e.errorCode)
        }
    }

    @Test
    fun `wrong password throws INVALID_CREDENTIALS`() = runTest {
        val email = "wrongpass-${System.currentTimeMillis()}@seguridad.mx"
        val password = "password123"

        Tasks.await(auth.createUserWithEmailAndPassword(email, password))

        try {
            Tasks.await(auth.signInWithEmailAndPassword(email, "wrongpassword"))
            assertTrue("Should have thrown FirebaseAuthException", false)
        } catch (e: FirebaseAuthException) {
            assertEquals("INVALID_CREDENTIALS", e.errorCode)
        }
    }

    @Test
    fun `sign out works`() = runTest {
        val email = "signout-${System.currentTimeMillis()}@seguridad.mx"
        val password = "password123"

        Tasks.await(auth.createUserWithEmailAndPassword(email, password))
        auth.signOut()
        assertNull(auth.currentUser)
    }
}