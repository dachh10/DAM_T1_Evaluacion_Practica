package com.example.dam_t1_evaluacion_practica.util

import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Tests for [EvidenceStorage] (design section 8).
 *
 * Contract:
 * - Files go to filesDir/evidence/{inspectionId}/{evidenceId}.jpg
 * - Directory is created if missing
 * - Returns a File ready for camera intent
 */
class EvidenceStorageTest {

    private val tempDir = File(System.getProperty("java.io.tmpdir"), "evidence-test-${System.currentTimeMillis()}")
    private lateinit var storage: EvidenceStorage

    @Before
    fun setup() {
        tempDir.mkdirs()
        storage = EvidenceStorage(tempDir)
    }

    @Test
    fun `getEvidenceFile creates nested directory structure`() {
        val inspectionId = "insp-123"
        val itemId = "epp_1"
        val evidenceId = EvidenceId.compute(inspectionId, itemId)

        val file = storage.getEvidenceFile(inspectionId, evidenceId)

        assertNotNull(file)
        assertTrue(file.absolutePath.contains("evidence"))
        assertTrue(file.absolutePath.contains(inspectionId))
        assertTrue(file.name == "$evidenceId.jpg")
        assertTrue(file.parentFile.exists())
    }

    @Test
    fun `different inspections get separate directories`() {
        val file1 = storage.getEvidenceFile("insp-1", EvidenceId.compute("insp-1", "epp_1"))
        val file2 = storage.getEvidenceFile("insp-2", EvidenceId.compute("insp-2", "epp_1"))

        assertNotNull(file1.parentFile)
        assertNotNull(file2.parentFile)
        assertTrue(file1.parentFile.absolutePath != file2.parentFile.absolutePath)
    }

    @Test
    fun `same evidenceId returns same file path`() {
        val inspectionId = "insp-1"
        val itemId = "epp_1"
        val evidenceId = EvidenceId.compute(inspectionId, itemId)

        val file1 = storage.getEvidenceFile(inspectionId, evidenceId)
        val file2 = storage.getEvidenceFile(inspectionId, evidenceId)

        assertEquals(file1.absolutePath, file2.absolutePath)
    }
}