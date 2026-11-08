package com.example.dam_t1_evaluacion_practica.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.unit.dp
import com.example.dam_t1_evaluacion_practica.util.RemoteImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Composable que muestra una evidencia desde URL remota (HTTPS) o local (content://).
 *
 * - HTTPS: usa [RemoteImageLoader] con cache LRU simple + produceState en IO
 * - content://: usa [GalleryUtils.loadBitmapFromUri] (legacy)
 * - Muestra progress mientras carga, error state si falla
 */
@Composable
fun EvidenceImage(
    modifier: Modifier = Modifier,
    url: String?,
    contentDescription: String = "Evidencia fotográfica",
    placeholder: @Composable (() -> Unit)? = null,
    errorFallback: @Composable (() -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Fit,
    size: androidx.compose.ui.unit.Dp = 64.dp
) {
    val context = LocalContext.current

    val bitmap by produceState(initialValue = null as Bitmap?, key1 = url) {
        if (url.isNullOrBlank()) {
            value = null
            return@produceState
        }

        if (url.startsWith("content://")) {
            // Local legacy
            value = withContext(Dispatchers.IO) {
                com.example.dam_t1_evaluacion_practica.util.GalleryUtils.loadBitmapFromUri(context, android.net.Uri.parse(url))
            }
            return@produceState
        }

        // Remote HTTPS
        val bytes = withContext(Dispatchers.IO) {
            RemoteImageLoader.load(url, context)
        }
        bytes?.let {
            value = BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.size(size)
            )
        } else if (url.isNullOrBlank()) {
            placeholder?.invoke()
        } else {
            // Loading or error
            if (url.startsWith("https://")) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            } else {
                errorFallback?.invoke() ?: placeholder?.invoke()
            }
        }
    }
}