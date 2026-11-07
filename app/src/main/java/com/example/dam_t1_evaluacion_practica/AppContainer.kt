package com.example.dam_t1_evaluacion_practica

import android.content.Context
import com.example.dam_t1_evaluacion_practica.data.local.AppDatabase
import com.example.dam_t1_evaluacion_practica.data.remote.auth.FirebaseAuthDataSource
import com.example.dam_t1_evaluacion_practica.data.remote.firestore.FirestoreInspectionDataSource
import com.example.dam_t1_evaluacion_practica.data.remote.firestore.RemoteInspectionDataSource
import com.example.dam_t1_evaluacion_practica.data.repository.FirebaseAuthRepository
import com.example.dam_t1_evaluacion_practica.data.repository.InspectionRepositoryImpl
import com.example.dam_t1_evaluacion_practica.data.sync.SyncCoordinator
import com.example.dam_t1_evaluacion_practica.data.sync.SyncScheduler
import com.example.dam_t1_evaluacion_practica.domain.repository.AuthRepository
import com.example.dam_t1_evaluacion_practica.domain.repository.InspectionRepository

/**
 * Manual composition root. S1 wires the Room-backed inspection repository; auth, remote,
 * evidence and sync collaborators are added by the later slices.
 */
class AppContainer(context: Context) {

    val db: AppDatabase = AppDatabase.getDatabase(context)

    val auth: AuthRepository = FirebaseAuthRepository(
        dataSource = FirebaseAuthDataSource(),
        profileDao = db.profileDao()
    )

    val remote: RemoteInspectionDataSource = FirestoreInspectionDataSource.create()
    val scheduler: SyncScheduler = SyncScheduler(context)
    val coordinator: SyncCoordinator = SyncCoordinator(
        inspectionDao = db.inspectionDao(),
        incidenceDao = db.incidenceDao(),
        remote = remote,
        scheduler = scheduler
    )

    val inspection: InspectionRepository = InspectionRepositoryImpl(
        inspectionDao = db.inspectionDao(),
        incidenceDao = db.incidenceDao(),
        userDao = db.userDao(),
        evidenceDao = db.evidenceDao(),
        remote = remote,
        coordinator = coordinator
    )
}
