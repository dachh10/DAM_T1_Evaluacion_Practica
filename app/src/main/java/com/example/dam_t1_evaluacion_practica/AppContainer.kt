package com.example.dam_t1_evaluacion_practica

import android.content.Context
import com.example.dam_t1_evaluacion_practica.data.local.AppDatabase

/**
 * Manual composition root. S0 exposes only the database; auth, remote, evidence and sync
 * collaborators are added by the later slices.
 */
class AppContainer(context: Context) {

    val db: AppDatabase = AppDatabase.getDatabase(context)
}
