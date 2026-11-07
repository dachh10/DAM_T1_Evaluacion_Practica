package com.example.dam_t1_evaluacion_practica.data.mapper

import com.example.dam_t1_evaluacion_practica.data.local.entity.ProfileEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.UserEntity
import com.example.dam_t1_evaluacion_practica.domain.model.User

fun ProfileEntity.toDomainModel(): User {
    return User(
        email = email,
        name = name,
        cedula = cedula,
        createdAt = createdAt,
        uid = uid
    )
}

fun User.toProfileEntity(updatedAt: Long = createdAt): ProfileEntity {
    return ProfileEntity(
        uid = uid,
        email = email,
        name = name,
        cedula = cedula,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Transitional mapping for the legacy credential table that still backs login in this
 * slice. Firebase Auth replaces it in a later slice, together with [UserEntity].
 */
fun UserEntity.toDomainModel(): User {
    return User(
        email = email,
        name = name,
        cedula = cedula,
        createdAt = createdAt
    )
}

/**
 * Transitional mapping for the legacy credential table that still backs registration in
 * this slice. Firebase Auth replaces it in a later slice, together with [UserEntity].
 */
fun User.toEntity(password: String): UserEntity {
    return UserEntity(
        email = email,
        name = name,
        password = password,
        cedula = cedula,
        createdAt = createdAt
    )
}
