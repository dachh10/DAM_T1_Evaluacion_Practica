package com.example.dam_t1_evaluacion_practica.util

import android.content.Context
import java.io.File

/**
 * Manages the local file storage for evidence photos.
 *
 * Files are stored at: filesDir/evidence/{inspectionId}/{evidenceId}.jpg
 * This path is used by FileProvider for camera intents and by the upload worker.
 */
class EvidenceStorage(private val baseDir: File) {

    /**
     * Creates an EvidenceStorage using the app's internal files directory.
     */
    constructor(context: Context) : this(context.filesDir)

    /**
     * Returns the file for a given evidence asset. Creates parent directories if needed.
     *
     * @param inspectionId the parent inspection ID
     * @param evidenceId the deterministic evidence ID (from [EvidenceId.compute])
     * @return a File ready to be passed to a camera intent via FileProvider
     */
    fun getEvidenceFile(inspectionId: String, evidenceId: String): File {
        val dir = File(baseDir, "evidence/$inspectionId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "$evidenceId.jpg")
    }
}