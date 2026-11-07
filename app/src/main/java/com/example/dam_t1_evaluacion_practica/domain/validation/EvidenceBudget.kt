package com.example.dam_t1_evaluacion_practica.domain.validation

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Evidence compression budget (design section 8).
 *
 * Constraints:
 * - longest edge ≤ 2048 px
 * - JPEG quality 80, floor 70
 * - hard cap 2 MiB (2,000,000 bytes)
 * - if over cap: reduce edge 1600 → 1280 → 1024 at q70, never upscale
 *
 * This is a pure computation; the actual bitmap compression is in [EvidencePreparer].
 */
object EvidenceBudget {

    private const val MAX_EDGE = 2048
    private const val MAX_SIZE_BYTES = 2_000_000L
    private const val INITIAL_QUALITY = 80
    private const val MIN_QUALITY = 70

    private val LADDER_EDGES = intArrayOf(2048, 1600, 1280, 1024)

    data class Result(
        val targetWidth: Int,
        val targetHeight: Int,
        val quality: Int,
        val estimatedSizeBytes: Long
    )

    /**
     * Computes the target dimensions and quality for a given source image.
     *
     * @param sourceWidth  original image width in pixels
     * @param sourceHeight original image height in pixels
     * @param sourceSizeBytes  original file size in bytes (if known, used to avoid unnecessary downscaling)
     * @return target dimensions, quality, and estimated output size
     */
    fun compute(
        sourceWidth: Int,
        sourceHeight: Int,
        sourceSizeBytes: Long = 0
    ): Result {
        // Step 1: clamp to max edge
        var width = sourceWidth
        var height = sourceHeight
        if (max(width, height) > MAX_EDGE) {
            if (width > height) {
                height = (height * MAX_EDGE / width).coerceAtLeast(1)
                width = MAX_EDGE
            } else {
                width = (width * MAX_EDGE / height).coerceAtLeast(1)
                height = MAX_EDGE
            }
        }

        // If original size is known and already under budget, don't downscale further
        if (sourceSizeBytes > 0 && sourceSizeBytes <= MAX_SIZE_BYTES) {
            val estimated = estimateJpegSize(width, height, INITIAL_QUALITY, sourceSizeBytes)
            return Result(width, height, INITIAL_QUALITY, estimated)
        }

        // Step 2: estimate size at initial quality
        var quality = INITIAL_QUALITY
        var estimatedSize = estimateJpegSize(width, height, quality, sourceSizeBytes)

        // Step 3: if over budget, walk the ladder
        if (estimatedSize > MAX_SIZE_BYTES) {
            for (ladderEdge in LADDER_EDGES) {
                if (ladderEdge >= max(width, height)) continue
                val scale = ladderEdge.toDouble() / max(width, height)
                val newWidth = (width * scale).roundToInt().coerceAtLeast(1)
                val newHeight = (height * scale).roundToInt().coerceAtLeast(1)
                estimatedSize = estimateJpegSize(newWidth, newHeight, MIN_QUALITY, sourceSizeBytes)
                if (estimatedSize <= MAX_SIZE_BYTES) {
                    return Result(newWidth, newHeight, MIN_QUALITY, estimatedSize)
                }
                width = newWidth
                height = newHeight
            }
            // Last resort: smallest ladder edge at min quality
            val lastEdge = LADDER_EDGES.last()
            val scale = lastEdge.toDouble() / max(width, height)
            width = (width * scale).roundToInt().coerceAtLeast(1)
            height = (height * scale).roundToInt().coerceAtLeast(1)
            quality = MIN_QUALITY
            estimatedSize = estimateJpegSize(width, height, quality, sourceSizeBytes)
        }

        return Result(width, height, quality, estimatedSize)
    }

    private fun estimateJpegSize(width: Int, height: Int, quality: Int, sourceSizeBytes: Long): Long {
        if (sourceSizeBytes > 0) {
            // If source size is known, use it as upper bound (original is uncompressed/larger)
            return sourceSizeBytes.coerceAtMost(MAX_SIZE_BYTES)
        }
        // Fallback heuristic: ~0.15 bytes per pixel at q80 (JPEG compression ~10:1 for photos)
        // At 2048x1536 = 3.1M pixels * 0.15 ≈ 470KB at q80
        return (width * height * quality * 3 / 16000).toLong().coerceAtMost(MAX_SIZE_BYTES + 1)
    }
}