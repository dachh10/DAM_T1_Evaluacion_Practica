package com.example.dam_t1_evaluacion_practica

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.rules.ExternalResource

/**
 * JUnit rule that configures Firebase to use the local emulators.
 *
 * Requires the emulators to be running:
 *   firebase emulators:start --only auth,firestore
 *
 * Usage in test class:
 *   @ClassRule
 *   @JvmStatic
 *   val emulatorRule = FirebaseEmulatorRule()
 */
class FirebaseEmulatorRule : ExternalResource() {

    override fun before() {
        // Configure Firestore to use emulator
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setHost("10.0.2.2:8080")
            .setSslEnabled(false)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

        // Configure Auth to use emulator
        FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
    }

    override fun after() {
        // Clear emulator data between tests if needed
    }
}