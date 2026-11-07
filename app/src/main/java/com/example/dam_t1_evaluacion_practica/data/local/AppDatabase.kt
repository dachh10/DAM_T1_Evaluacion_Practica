package com.example.dam_t1_evaluacion_practica.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dam_t1_evaluacion_practica.data.local.dao.EvidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.IncidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.InspectionDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.ProfileDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.UserDao
import com.example.dam_t1_evaluacion_practica.data.local.entity.EvidenceEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.IncidenceEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.InspectionEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.ProfileEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.UserEntity

@Database(
    entities = [
        InspectionEntity::class,
        IncidenceEntity::class,
        UserEntity::class,
        ProfileEntity::class,
        EvidenceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao
    abstract fun incidenceDao(): IncidenceDao
    abstract fun userDao(): UserDao
    abstract fun profileDao(): ProfileDao
    abstract fun evidenceDao(): EvidenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inspection_database"
                )
                    // Version 3 moves the inspection primary key to the cloud id, so the
                    // local schema starts clean instead of backfilling UUIDs per row.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
