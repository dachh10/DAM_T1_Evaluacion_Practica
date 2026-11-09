package com.example.dam_t1_evaluacion_practica.data.remote.cloudinary

import com.example.dam_t1_evaluacion_practica.BuildConfig
import com.example.dam_t1_evaluacion_practica.domain.model.Evidence
import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for [CloudinaryEvidenceUploader] (design section 8).
 *
 * These verify the public_id format and preset constant without requiring
 * a live Cloudinary connection or complex mocking of MediaManager.
 */
class CloudinaryEvidenceUploaderTest {

    @Test
    fun `public_id format is inspections inspectionId evidenceId`() {
        val evidence = Evidence(
            evidenceId = "ev-123",
            inspectionId = "insp-1",
            itemId = "epp_1",
            localRef = "file:///data/.../ev-123.jpg",
            remoteUrl = null,
            status = EvidenceStatus.PENDING,
            attempts = 0,
            lastError = null
        )

        val publicId = CloudinaryEvidenceUploader.buildPublicId(evidence)

        assertEquals("inspections/insp-1/ev-123", publicId)
    }

    @Test
    fun `preset comes from the build configuration`() {
        assertEquals(BuildConfig.CLOUDINARY_UPLOAD_PRESET, CloudinaryEvidenceUploader.PRESET)
        assertEquals(BuildConfig.CLOUDINARY_CLOUD_NAME, CloudinaryEvidenceUploader.CLOUD_NAME)
    }

    @Test
    fun `public_id differs for different inspectionId`() {
        val evidence1 = Evidence(evidenceId = "ev-1", inspectionId = "insp-1", itemId = "epp_1", localRef = "file", remoteUrl = null, status = EvidenceStatus.PENDING, attempts = 0, lastError = null)
        val evidence2 = Evidence(evidenceId = "ev-1", inspectionId = "insp-2", itemId = "epp_1", localRef = "file", remoteUrl = null, status = EvidenceStatus.PENDING, attempts = 0, lastError = null)

        val id1 = CloudinaryEvidenceUploader.buildPublicId(evidence1)
        val id2 = CloudinaryEvidenceUploader.buildPublicId(evidence2)

        assertTrue("different inspectionId should give different public_id", id1 != id2)
    }

    @Test
    fun `public_id differs for different evidenceId`() {
        val evidence1 = Evidence(evidenceId = "ev-1", inspectionId = "insp-1", itemId = "epp_1", localRef = "file", remoteUrl = null, status = EvidenceStatus.PENDING, attempts = 0, lastError = null)
        val evidence2 = Evidence(evidenceId = "ev-2", inspectionId = "insp-1", itemId = "epp_1", localRef = "file", remoteUrl = null, status = EvidenceStatus.PENDING, attempts = 0, lastError = null)

        val id1 = CloudinaryEvidenceUploader.buildPublicId(evidence1)
        val id2 = CloudinaryEvidenceUploader.buildPublicId(evidence2)

        assertTrue("different evidenceId should give different public_id", id1 != id2)
    }
}