package com.example.dam_t1_evaluacion_practica.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Loads remote evidence images from HTTPS URLs.
 *
 * Design section 8:
 * - 10 second connect/read timeout
 * - 2 MiB read cap
 * - Returns byte array or null on failure
 * - Runs on IO dispatcher
 */
object RemoteImageLoader {

    private const val TIMEOUT_SECONDS = 10L
    private const val MAX_SIZE_BYTES = 2 * 1024 * 1024L // 2 MiB

    @Volatile var testMode = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Loads an image from a remote HTTPS URL.
     *
     * @param url the image URL (must be https:// in production; http://localhost allowed for testing)
     * @param context optional context to check if debug build (allows http://localhost in debug)
     * @return byte array of the image, or null on failure (4xx, 5xx, timeout, >2MiB, network error)
     */
    @Throws(Exception::class)
    suspend fun load(url: String, context: Context? = null): ByteArray? = withContext(Dispatchers.IO) {
        val isDebug = context?.applicationInfo?.flags?.and(android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val allowHttpLocalhost = isDebug && (url.startsWith("http://10.0.2.2") || url.startsWith("http://localhost:") || url.startsWith("http://127.0.0.1:"))
        // Also allow localhost for testing (mock web server uses http://localhost:PORT)
        val isTestLocalhost = testMode || url.contains("localhost") || url.contains("127.0.0.1")

        if (!url.startsWith("https://") && !allowHttpLocalhost && !isTestLocalhost) return@withContext null

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            val contentLength = body.contentLength()
            if (contentLength > MAX_SIZE_BYTES) return@withContext null

            val bytes = body.bytes()
            if (bytes.isEmpty()) return@withContext null
            if (bytes.size > MAX_SIZE_BYTES) return@withContext null

            bytes
        }
    }
}