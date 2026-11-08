package com.example.dam_t1_evaluacion_practica

import android.app.Application
import com.example.dam_t1_evaluacion_practica.data.remote.cloudinary.CloudinaryEvidenceUploader

/**
 * Application entry point. Owns the manual composition root so WorkManager workers can
 * resolve collaborators after process death, where no Activity exists.
 *
 * The container is built lazily on the first access, on whichever thread asks for it:
 * the UI builds it from a background dispatcher behind the splash screen, and a worker
 * builds it on its own background thread. `Application.onCreate` therefore stays cheap
 * and never blocks the first frame.
 */
class SecurityInspectionsApp : Application() {

    private val containerDelegate = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContainer(this)
    }

    val container: AppContainer get() = containerDelegate.value

    override fun onCreate() {
        super.onCreate()
        CloudinaryEvidenceUploader.initialize(this)
    }
}
