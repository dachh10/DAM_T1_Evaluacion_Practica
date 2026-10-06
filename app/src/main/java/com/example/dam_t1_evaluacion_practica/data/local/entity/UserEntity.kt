package com.example.dam_t1_evaluacion_practica.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val password: String,
    val cedula: String,
    val createdAt: Long = System.currentTimeMillis()
)
