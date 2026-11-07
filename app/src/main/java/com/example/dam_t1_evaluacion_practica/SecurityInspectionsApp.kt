package com.example.dam_t1_evaluacion_practica

import android.app.Application

/**
 * Application entry point. Owns the manual composition root so WorkManager workers can
 * resolve collaborators after process death, where no Activity exists.
 */
class SecurityInspectionsApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
