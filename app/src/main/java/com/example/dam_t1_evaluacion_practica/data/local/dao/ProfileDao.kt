package com.example.dam_t1_evaluacion_practica.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dam_t1_evaluacion_practica.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE uid = :uid LIMIT 1")
    fun observeProfile(uid: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE uid = :uid LIMIT 1")
    suspend fun getCachedProfile(uid: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getByEmail(email: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles")
    suspend fun clearProfiles()
}
