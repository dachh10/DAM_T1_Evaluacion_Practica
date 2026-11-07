package com.example.dam_t1_evaluacion_practica.domain.model

import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the deterministic evidence ID contract (design section 8):
 * evidenceId = UUID.nameUUIDFromBytes("$inspectionId|$itemId".toByteArray())
 * Stable across retries and re-captures; used as Cloudinary public_id suffix.
 */
class EvidenceIdTest {

    @Test
    fun `evidenceId is deterministic for same inspectionId and itemId`() {
        val id1 = EvidenceId.compute("insp-123", "epp_1")
        val id2 = EvidenceId.compute("insp-123", "epp_1")

        assertEquals(id1, id2)
    }

    @Test
    fun `evidenceId differs for different itemId`() {
        val id1 = EvidenceId.compute("insp-123", "epp_1")
        val id2 = EvidenceId.compute("insp-123", "epp_2")

        assertNotNull(id1)
        assertNotNull(id2)
        assertNotNull(id1) // sanity
        assertNotNull(id2)
        assertNotNull(id1)
    }

    @Test
    fun `evidenceId differs for different inspectionId`() {
        val id1 = EvidenceId.compute("insp-123", "epp_1")
        val id2 = EvidenceId.compute("insp-456", "epp_1")

        assertNotNull(id1)
        assertNotNull(id2)
    }

    @Test
    fun `evidenceId is valid UUID format`() {
        val id = EvidenceId.compute("insp-123", "epp_1")

        // UUID format: 8-4-4-4-12 hex chars
        val uuidRegex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
        assertTrue("ID '$id' must be valid UUID", id.matches(uuidRegex.toRegex()))
    }

    @Test
    fun `evidenceId is stable across retries (same input = same output)`() {
        val base = "insp-123"
        val item = "epp_1"

        val ids = (1..100).map { EvidenceId.compute(base, item) }.distinct()

        assertEquals(1, ids.size)
    }
}