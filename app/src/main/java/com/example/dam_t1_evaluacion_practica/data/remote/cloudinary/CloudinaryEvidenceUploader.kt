package com.example.dam_t1_evaluacion_practica.data.remote.cloudinary

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.dam_t1_evaluacion_practica.BuildConfig
import com.example.dam_t1_evaluacion_practica.domain.model.Evidence
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.UUID

/**
 * Uploads evidence photos to Cloudinary using an unsigned preset.
 *
 * Design section 8:
 * - public_id = "inspections/{inspectionId}/{evidenceId}"
 * - unsigned preset from BuildConfig (no API secret in the client)
 * - Returns the Cloudinary `secure_url` on success, null on failure
 *
 * `MediaManager.init()` is performed lazily on the first upload, off the main thread,
 * so app startup is never blocked by it.
 */
class CloudinaryEvidenceUploader private constructor(
    private val appContext: Context
) {

    companion object {
        private const val UPLOAD_TIMEOUT_MS = 120_000L
        private const val TEMP_FILE_PREFIX = "cloudinary-upload-"

        /** Cloudinary cloud name, injected from `local.properties` into BuildConfig. */
        val CLOUD_NAME: String get() = BuildConfig.CLOUDINARY_CLOUD_NAME

        /** Unsigned upload preset, injected from `local.properties` into BuildConfig. */
        val PRESET: String get() = BuildConfig.CLOUDINARY_UPLOAD_PRESET

        @Volatile
        private var INSTANCE: CloudinaryEvidenceUploader? = null

        @Volatile
        private var appContext: Context? = null

        @Volatile
        private var mediaManagerReady = false

        /**
         * Prepares the uploader. Idempotent and cheap: it only keeps the application
         * context. The real `MediaManager.init` happens the first time an upload runs.
         */
        fun initialize(context: Context) {
            appContext = context.applicationContext
        }

        fun getInstance(context: Context): CloudinaryEvidenceUploader {
            val resolved = appContext ?: context.applicationContext.also { appContext = it }
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CloudinaryEvidenceUploader(resolved).also { INSTANCE = it }
            }
        }

        private fun mediaManager(context: Context): MediaManager {
            if (!mediaManagerReady) {
                synchronized(this) {
                    if (!mediaManagerReady) {
                        MediaManager.init(
                            context,
                            mapOf<String, Any>("cloud_name" to CLOUD_NAME, "secure" to true)
                        )
                        mediaManagerReady = true
                    }
                }
            }
            return MediaManager.get()
        }

        fun buildPublicId(evidence: Evidence): String =
            "inspections/${evidence.inspectionId}/${evidence.evidenceId}"
    }

    /**
     * Uploads an evidence photo to Cloudinary and waits for the asynchronous result.
     *
     * @param evidence the evidence to upload; [Evidence.localRef] may be a `content://`
     * URI, a `file://` URI or an absolute file path.
     * @return the Cloudinary `secure_url` on success, null on any failure or timeout.
     */
    suspend fun upload(evidence: Evidence): String? = withContext(Dispatchers.IO) {
        if (CLOUD_NAME.isBlank() || PRESET.isBlank()) return@withContext null

        val uploadFile = resolveUploadFile(evidence.localRef) ?: return@withContext null
        try {
            val deferred = CompletableDeferred<String?>()

            mediaManager(appContext)
                .upload(uploadFile.absolutePath)
                .unsigned(PRESET)
                .option("public_id", buildPublicId(evidence))
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) = Unit

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) = Unit

                    override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                        deferred.complete(resultData?.get("secure_url") as? String)
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        deferred.complete(null)
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                        deferred.complete(null)
                    }
                })
                .dispatch()

            withTimeoutOrNull(UPLOAD_TIMEOUT_MS) { deferred.await() }
        } catch (e: Exception) {
            null
        } finally {
            if (uploadFile.name.startsWith(TEMP_FILE_PREFIX)) {
                uploadFile.delete()
            }
        }
    }

    /**
     * Resolves the local reference to a readable file. Content URIs (the gallery URI the
     * camera flow produces) are copied into the cache because the Cloudinary SDK needs a
     * real file; the copy is deleted after the upload.
     */
    private fun resolveUploadFile(localRef: String): File? = try {
        when {
            localRef.startsWith("content://") -> {
                val temp = File(
                    appContext.cacheDir,
                    "$TEMP_FILE_PREFIX${UUID.randomUUID()}.jpg"
                )
                val input = appContext.contentResolver.openInputStream(Uri.parse(localRef))
                if (input == null) {
                    null
                } else {
                    input.use { source ->
                        temp.outputStream().use { target -> source.copyTo(target) }
                    }
                    temp.takeIf { it.length() > 0 }
                }
            }

            localRef.startsWith("file://") -> Uri.parse(localRef).path?.let(::File)
            else -> File(localRef)
        }?.takeIf { it.exists() && it.length() > 0 }
    } catch (e: Exception) {
        null
    }
}
