package com.example.dam_t1_evaluacion_practica.domain.validation

import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceStatus
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the evidence budget contract (design section 8):
 * - longest edge ≤ 2048 px
 * - JPEG quality 80, floor 70
 * - hard cap 2 MiB
 * - if over cap: reduce edge 1600 → 1280 → 1024 at q70, never upscale
 */
class EvidenceBudgetTest {

    @Test
    fun `budget allows up to 2048 px on longest edge`() {
        val result = EvidenceBudget.compute(2048, 1536, 1_500_000)

        assertEquals(2048, result.targetWidth)
        assertEquals(1536, result.targetHeight)
        assertEquals(80, result.quality)
        assertTrue(result.estimatedSizeBytes <= 2_000_000)
    }

    @Test
    fun `budget downscales when over 2048 px`() {
        val result = EvidenceBudget.compute(4000, 3000, 8_000_000)

        assertTrue("longest edge must be ≤ 2048", result.targetWidth <= 2048 && result.targetHeight <= 2048)
        assertTrue(result.estimatedSizeBytes <= 2_000_000)
    }

    @Test
    fun `budget never upscales small images`() {
        val result = EvidenceBudget.compute(800, 600, 500_000)

        assertEquals(800, result.targetWidth)
        assertEquals(600, result.targetHeight)
        assertEquals(80, result.quality)
    }

    @Test
    fun `budget reduces quality floor to 70 before second downscale`() {
        val result = EvidenceBudget.compute(4000, 3000, 8_000_000)

        assertTrue(result.quality >= 70)
        assertTrue(result.quality <= 80)
    }

    @Test
    fun `budget ladder 2048@80 then 1600@70 then 1280@70 then 1024@70`() {
        val result = EvidenceBudget.compute(5000, 4000, 15_000_000)

        val edge = max(result.targetWidth, result.targetHeight)
        assertTrue(edge == 2048 || edge == 1600 || edge == 1280 || edge == 1024)
        assertTrue(result.quality >= 70 && result.quality <= 80)
        assertTrue(result.estimatedSizeBytes <= 2_000_000)
    }
}