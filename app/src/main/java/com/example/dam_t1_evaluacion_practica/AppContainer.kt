package com.example.dam_t1_evaluacion_practica

import android.content.Context
import com.example.dam_t1_evaluacion_practica.data.local.AppDatabase
import com.example.dam_t1_evaluacion_practica.data.repository.InspectionRepositoryImpl
import com.example.dam_t1_evaluacion_practica.domain.repository.InspectionRepository

/**
 * Manual composition root. S1 wires the Room-backed inspection repository; auth, remote,
 * evidence and sync collaborators are added by the later slices.
 */
class AppContainer(context: Context) {

    val db: AppDatabase = AppDatabase.getDatabase(context)

    val inspection: InspectionRepository = InspectionRepositoryImpl(
        inspectionDao = db.inspectionDao(),
        incidenceDao = db.incidenceDao(),
        userDao = db.userDao(),
        evidenceDao = db.evidenceDao()
    )
}
