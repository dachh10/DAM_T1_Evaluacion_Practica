package com.example.dam_t1_evaluacion_practica.data.mapper

import com.example.dam_t1_evaluacion_practica.data.local.entity.ProfileEntity
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