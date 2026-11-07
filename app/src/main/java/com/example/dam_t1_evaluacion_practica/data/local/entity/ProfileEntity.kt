package com.example.dam_t1_evaluacion_practica.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of the signed-in inspector's profile.
 *
 * This table holds no credential: it exists so the app can render the user offline while
 * Firebase owns authentication. It replaces the credential-bearing `users` table in a
 * later slice.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val name: String,
    val cedula: String,
    val createdAt: Long,
    val updatedAt: Long
)
