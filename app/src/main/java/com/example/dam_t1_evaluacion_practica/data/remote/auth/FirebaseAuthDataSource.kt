package com.example.dam_t1_evaluacion_practica.data.remote.auth

import com.example.dam_t1_evaluacion_practica.domain.error.AuthError
import com.example.dam_t1_evaluacion_practica.domain.error.AuthException
import com.example.dam_t1_evaluacion_practica.domain.model.AuthSession
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Adapter over Firebase Email/Password auth (design section 9).
 *
 * All failures cross this boundary as [AuthException]; SDK types and raw error codes
 * never leave this package. Tasks are bridged to coroutines by the local [awaitTask]
 * helper, so no `kotlinx-coroutines-play-services` dependency is required.
 */
class FirebaseAuthDataSource(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /** Emits the restored/current session and every later sign-in/sign-out transition. */
    val session: Flow<AuthSession?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toSession())
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.toSession())
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Uid currently signed in, or null when signed out. */
    val currentUid: String? get() = auth.currentUser?.uid

    /** Signs in with email/password and returns the established session. */
    suspend fun signIn(email: String, password: String): AuthSession {
        val result = awaitAuthTask(auth.signInWithEmailAndPassword(email, password))
        return result?.user?.toSession() ?: throw AuthException(AuthError.UNKNOWN)
    }

    /** Creates the Auth account and returns the established session. */
    suspend fun register(email: String, password: String): AuthSession {
        val result = awaitAuthTask(auth.createUserWithEmailAndPassword(email, password))
        return result?.user?.toSession() ?: throw AuthException(AuthError.UNKNOWN)
    }

    /** Clears the persisted session; Firebase handles the disk cache. */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Bridges a Google Play Services [Task] to a suspending call.
     * Cancellation of the coroutine is propagated; the underlying task is not cancelled.
     */
    private suspend fun <T> awaitTask(task: Task<T>): T? =
        suspendCancellableCoroutine { continuation ->
            task.addOnCompleteListener { completed ->
                if (!continuation.isActive) return@addOnCompleteListener
                when {
                    completed.isCanceled -> continuation.cancel()
                    completed.isSuccessful -> continuation.resume(completed.result)
                    else -> continuation.resumeWithException(
                        completed.exception ?: IllegalStateException("Auth task failed")
                    )
                }
            }
        }

    /**
     * Same bridge, but translates SDK failures into [AuthException] using [AuthErrorMapper].
     * Any non-SDK throwable is reported as [AuthError.UNKNOWN]; coroutine cancellation
     * is rethrown untouched.
     */
    private suspend fun <T> awaitAuthTask(task: Task<T>): T? {
        return try {
            awaitTask(task)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (network: FirebaseNetworkException) {
            // FirebaseNetworkException extends FirebaseException, not FirebaseAuthException;
            // without this branch it would fall through to UNKNOWN and hide the real cause.
            throw AuthException(AuthError.NETWORK_UNAVAILABLE)
        } catch (authError: FirebaseAuthException) {
            throw AuthException(AuthErrorMapper.fromErrorCode(authError.errorCode))
        } catch (throwable: Throwable) {
            throw AuthException(AuthError.UNKNOWN)
        }
    }

    private fun FirebaseUser.toSession(): AuthSession = AuthSession(
        uid = uid,
        email = email.orEmpty()
    )
}
