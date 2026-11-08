package com.example.dam_t1_evaluacion_practica.domain.repository

import com.example.dam_t1_evaluacion_practica.domain.model.AuthSession
import com.example.dam_t1_evaluacion_practica.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Authentication contract. Implementations wrap the auth provider and the local
 * profile cache; no provider type crosses this boundary.
 */
interface AuthRepository {

    /** Emits the current authenticated session, or null when signed out. */
    val session: Flow<AuthSession?>

    /** Signs in and returns the inspector profile. Throws AuthException on failure. */
    suspend fun signIn(email: String, password: String): User

    /** Registers a new account and returns the inspector profile. */
    suspend fun register(name: String, email: String, cedula: String, password: String): User

    suspend fun signOut()

    fun observeProfile(uid: String): Flow<User?>

    suspend fun cachedProfile(uid: String): User?

    /** Looks up a cached profile by e-mail, case-insensitively. */
    suspend fun cachedProfileByEmail(email: String): User?
}
