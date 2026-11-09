package com.example.dam_t1_evaluacion_practica.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dam_t1_evaluacion_practica.data.local.dao.EvidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.IncidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.InspectionDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.ProfileDao
import com.example.dam_t1_evaluacion_practica.data.local.entity.EvidenceEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.IncidenceEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.InspectionEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.ProfileEntity

@Database(
    entities = [
        InspectionEntity::class,
        IncidenceEntity::class,
        ProfileEntity::class,
        EvidenceEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao
    abstract fun incidenceDao(): IncidenceDao
    abstract fun profileDao(): ProfileDao
    abstract fun evidenceDao(): EvidenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v5 adds the optional link from a reinspection to the dictamen it re-evaluates. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inspections ADD COLUMN parentInspectionId TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inspection_database"
                )
                    // Version 4 removes the legacy user table (moved to Firebase Auth);
                    // v5 keeps existing dictámenes while adding the reinspection link.
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}