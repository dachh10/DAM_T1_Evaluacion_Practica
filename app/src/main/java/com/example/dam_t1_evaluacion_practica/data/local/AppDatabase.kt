package com.example.dam_t1_evaluacion_practica.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dam_t1_evaluacion_practica.data.local.dao.IncidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.InspectionDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.UserDao
import com.example.dam_t1_evaluacion_practica.data.local.entity.IncidenceEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.InspectionEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.UserEntity
import java.util.concurrent.Executors

@Database(
    entities = [
        InspectionEntity::class,
        IncidenceEntity::class,
        UserEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao
    abstract fun incidenceDao(): IncidenceDao
    abstract fun userDao(): UserDao

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
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default demo user
                            Executors.newSingleThreadExecutor().execute {
                                val user = UserEntity(
                                    email = "inspector@seguridad.mx",
                                    name = "Ing. Rodrigo Morales",
                                    password = "password123",
                                    cedula = "STPS-AUD-2024-8841"
                                )
                                INSTANCE?.userDao()?.insertUser(user)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}