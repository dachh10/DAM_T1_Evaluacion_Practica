package com.example.dam_t1_evaluacion_practica.data.repository

import com.example.dam_t1_evaluacion_practica.data.local.dao.ProfileDao
import com.example.dam_t1_evaluacion_practica.data.mapper.toDomainModel
import com.example.dam_t1_evaluacion_practica.data.mapper.toProfileEntity
import com.example.dam_t1_evaluacion_practica.data.remote.auth.FirebaseAuthDataSource
import com.example.dam_t1_evaluacion_practica.domain.model.AuthSession
import com.example.dam_t1_evaluacion_practica.domain.model.User
import com.example.dam_t1_evaluacion_practica.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [AuthRepository] over the Firebase Auth adapter and the Room `profiles` cache
 * (design section 9).
 *
 * The session is the provider's own `callbackFlow`; the profile comes from the local
 * cache, so restoring a session never needs the network. Provider types and raw error
 * codes never cross this boundary: failures arrive as [AuthException].
 *
 * @param remoteProfileWriter writes the remote `users/{uid}` document. The sync slice
 * supplies the Firestore implementation; without it the profile is only cached locally.
 */
class FirebaseAuthRepository(
    private val dataSource: FirebaseAuthDataSource,
    private val profileDao: ProfileDao,
    private val remoteProfileWriter: suspend (User) -> Unit = {}
) : AuthRepository {

    override val session: Flow<AuthSession?> = dataSource.session

    override suspend fun signIn(email: String, password: String): User {
        val session = dataSource.signIn(email, password)
        return ensureProfile(session.uid, session.email)
    }

    override suspend fun register(name: String, email: String, cedula: String, password: String): User {
        val session = dataSource.register(email, password)
        val profile = User(
            email = session.email.ifBlank { email },
            name = name,
            cedula = cedula,
            uid = session.uid
        )
        writeProfile(profile)
        return profile
    }

    override suspend fun signOut() {
        dataSource.signOut()
    }

    override fun observeProfile(uid: String): Flow<User?> =
        profileDao.observeProfile(uid).map { it?.toDomainModel() }

    override suspend fun cachedProfile(uid: String): User? =
        profileDao.getCachedProfile(uid)?.toDomainModel()

    /**
     * Guarantees a cached profile for [uid] and returns it.
     *
     * An existing cache row keeps its display name and cedula; a missing one is rebuilt
     * from the session identity so the app still opens offline.
     */
    suspend fun ensureProfile(uid: String, email: String, name: String = "", cedula: String = ""): User {
        val cached = cachedProfile(uid)
        val profile = cached?.copy(email = email.ifBlank { cached.email })
            ?: User(email = email, name = name, cedula = cedula, uid = uid)
        writeProfile(profile)
        return profile
    }

    /**
     * Writes the remote profile document and the local cache row. A failed remote write
     * is not fatal: the account survives and the next sign-in retries it.
     */
    private suspend fun writeProfile(profile: User) {
        try {
            remoteProfileWriter(profile)
        } catch (cancellation: CancellationException) {
            // Cancellation is control flow, not a failure: it must not be swallowed.
            throw cancellation
        } catch (ignored: Exception) {
            // Non-fatal by design (design section 9).
        }
        profileDao.upsertProfile(profile.toProfileEntity(updatedAt = System.currentTimeMillis()))
    }
}
