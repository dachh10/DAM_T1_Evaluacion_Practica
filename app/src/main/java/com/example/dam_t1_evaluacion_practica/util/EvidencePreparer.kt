package com.example.dam_t1_evaluacion_practica.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.example.dam_t1_evaluacion_practica.domain.validation.EvidenceBudget
import java.io.File
import java.io.FileOutputStream

/**
 * Prepares evidence photos for upload: loads, rotates per EXIF, compresses to budget.
 *
 * Pipeline:
 * 1. Load bitmap with inJustDecodeBounds to get original dimensions
 * 2. Compute target dimensions/quality via [EvidenceBudget]
 * 3. Load full bitmap at target size (inSampleSize)
 * 4. Rotate per EXIF orientation
 * 5. Compress to JPEG at target quality
 * 6. Write to output file
 *
 * Never upscales. Keeps full-resolution original at source path; this produces
 * the upload-ready copy at target path.
 */
object EvidencePreparer {

    data class Result(
        val outputFile: File,
        val width: Int,
        val height: Int,
        val quality: Int,
        val sizeBytes: Long
    )

    /**
     * Prepares an evidence photo for upload.
     *
     * @param sourceFile   the original captured photo (full resolution)
     * @param targetFile   where to write the compressed JPEG
     * @param sourceSizeBytes size of source file in bytes (used for budget estimation)
     * @return result with output dimensions, quality, and file size
     */
    @Throws(Exception::class)
    fun prepare(
        sourceFile: File,
        targetFile: File,
        sourceSizeBytes: Long = sourceFile.length()
    ): Result {
        // Step 1: decode bounds to get original dimensions without loading pixels
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourceFile.absolutePath, boundsOptions)
        val srcWidth = boundsOptions.outWidth
        val srcHeight = boundsOptions.outHeight

        // Step 2: compute target dimensions and quality
        val budget = EvidenceBudget.compute(srcWidth, srcHeight, sourceSizeBytes)

        // Step 3: compute inSampleSize for efficient decoding
        val sampleSize = computeInSampleSize(srcWidth, srcHeight, budget.targetWidth, budget.targetHeight)

        // Step 4: decode full bitmap at target size
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decodedBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions)
            ?: throw IllegalStateException("Failed to decode image: ${sourceFile.absolutePath}")

        // Step 5: rotate per EXIF orientation
        val bitmap = rotateBitmapIfNeeded(decodedBitmap, sourceFile.absolutePath)

        // Step 6: compress to JPEG at target quality
        targetFile.parentFile?.mkdirs()
        val outputStream = FileOutputStream(targetFile)
        try {
            val success = bitmap.compress(Bitmap.CompressFormat.JPEG, budget.quality, outputStream)
            if (!success) {
                throw IllegalStateException("JPEG compression failed")
            }
        } finally {
            outputStream.close()
        }

        // Step 7: cleanup
        val resultSize = targetFile.length()
        bitmap.recycle()

        return Result(
            outputFile = targetFile,
            width = budget.targetWidth,
            height = budget.targetHeight,
            quality = budget.quality,
            sizeBytes = resultSize
        )
    }

    private fun computeInSampleSize(
        srcWidth: Int, srcHeight: Int,
        targetWidth: Int, targetHeight: Int
    ): Int {
        var inSampleSize = 1
        if (srcHeight > targetHeight || srcWidth > targetWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2
            while ((halfHeight / inSampleSize) >= targetHeight && (halfWidth / inSampleSize) >= targetWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, imagePath: String): Bitmap {
        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}