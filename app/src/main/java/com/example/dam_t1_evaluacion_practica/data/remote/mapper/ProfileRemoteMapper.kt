package com.example.dam_t1_evaluacion_practica.data.remote.mapper

import com.example.dam_t1_evaluacion_practica.domain.model.User

/**
 * Pure conversion between the inspector profile and its remote document shape
 * (design section 4: `users/{uid}`).
 *
 * Plain Kotlin only: no Firebase types, no `org.json`, no reflection. `updatedAt` is
 * server-owned (design section 4) and is therefore never written by this mapper.
 */
object ProfileRemoteMapper {

    const val KEY_EMAIL = "email"
    const val KEY_DISPLAY_NAME = "displayName"
    const val KEY_CEDULA = "cedula"
    const val KEY_CREATED_AT = "createdAt"

    /** Builds the remote document for [user]; it never contains a credential. */
    fun toMap(user: User): Map<String, Any?> = mapOf(
        KEY_EMAIL to user.email,
        KEY_DISPLAY_NAME to user.name,
        KEY_CEDULA to user.cedula,
        KEY_CREATED_AT to user.createdAt
    )

    /**
     * Rebuilds the profile from a remote document. The uid always comes from the
     * document id argument, missing keys fall back to empty strings and unknown keys
     * are ignored (design section 5).
     */
    fun fromMap(uid: String, map: Map<String, Any?>): User = User(
        email = map[KEY_EMAIL] as? String ?: "",
        name = map[KEY_DISPLAY_NAME] as? String ?: "",
        cedula = map[KEY_CEDULA] as? String ?: "",
        createdAt = (map[KEY_CREATED_AT] as? Number)?.toLong() ?: 0L,
        uid = uid
    )
}
