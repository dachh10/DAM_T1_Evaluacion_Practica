package com.example.dam_t1_evaluacion_practica.data.mapper

import com.example.dam_t1_evaluacion_practica.data.local.entity.UserEntity
import com.example.dam_t1_evaluacion_practica.domain.model.User

fun UserEntity.toDomainModel(): User {
    return User(
        email = email,
        name = name,
        cedula = cedula,
        createdAt = createdAt
    )
}

fun User.toEntity(password: String): UserEntity {
    return UserEntity(
        email = email,
        name = name,
        password = password,
        cedula = cedula,
        createdAt = createdAt
    )
}
