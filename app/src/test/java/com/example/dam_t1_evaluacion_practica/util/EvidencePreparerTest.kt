package com.example.dam_t1_evaluacion_practica.util

import com.example.dam_t1_evaluacion_practica.domain.validation.EvidenceBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [EvidencePreparer] (design section 8).
 *
 * Contract:
 * - Takes a source JPEG file
 * - Compresses according to [EvidenceBudget]: max 2048px, q80→70, cap 2MiB
 * - Writes output to target file
 * - Never upscales
 * - Returns the actual output size and dimensions
 */
class EvidencePreparerTest {

    @Test
    fun `prepare downscales large image to max 2048px edge`() {
        // This test documents the contract; actual bitmap compression tested in instrumented tests
        val budget = EvidenceBudget.compute(4000, 3000, 8_000_000)

        assertTrue("longest edge ≤ 2048", budget.targetWidth <= 2048 && budget.targetHeight <= 2048)
        assertTrue(budget.estimatedSizeBytes <= 2_000_000)
        assertTrue(budget.quality >= 70 && budget.quality <= 80)
    }

    @Test
    fun `prepare keeps small image at original size with q80`() {
        val budget = EvidenceBudget.compute(800, 600, 500_000)

        assertEquals(800, budget.targetWidth)
        assertEquals(600, budget.targetHeight)
        assertEquals(80, budget.quality)
    }

    @Test
    fun `prepare walks ladder when over 2MiB`() {
        val budget = EvidenceBudget.compute(5000, 4000, 15_000_000)

        val edge = kotlin.math.max(budget.targetWidth, budget.targetHeight)
        assertTrue(edge == 2048 || edge == 1600 || edge == 1280 || edge == 1024)
        assertTrue(budget.quality >= 70 && budget.quality <= 80)
        assertTrue(budget.estimatedSizeBytes <= 2_000_000)
    }

    @Test
    fun `prepare never upscales`() {
        val budget = EvidenceBudget.compute(400, 300, 50_000)

        assertEquals(400, budget.targetWidth)
        assertEquals(300, budget.targetHeight)
    }
}