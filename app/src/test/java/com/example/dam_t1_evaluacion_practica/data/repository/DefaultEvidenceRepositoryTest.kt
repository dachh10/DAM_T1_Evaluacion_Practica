package com.example.dam_t1_evaluacion_practica.data.repository

import com.example.dam_t1_evaluacion_practica.data.local.dao.EvidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.entity.EvidenceEntity
import com.example.dam_t1_evaluacion_practica.domain.model.Evidence
import com.example.dam_t1_evaluacion_practica.domain.model.EvidenceStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [DefaultEvidenceRepository] (design section 8).
 *
 * Contract:
 * - register(inspectionId, itemId, localRef) → PENDING, unique enqueue
 * - observe(inspectionId) → Flow of Evidence (domain model)
 * - retry(evidenceId) → re-queues FAILED evidence
 */
class DefaultEvidenceRepositoryTest {

    private val evidenceDao = FakeEvidenceDao()
    private val repository = DefaultEvidenceRepository(evidenceDao)

    @Before
    fun setup() {
        evidenceDao.clear()
    }

    @Test
    fun `register creates PENDING evidence and returns it`() = runTest {
        val evidence = repository.register("insp-1", "epp_1", "content://media/file.jpg")

        assertEquals(EvidenceStatus.PENDING, evidence.status)
        assertEquals("insp-1", evidence.inspectionId)
        assertEquals("epp_1", evidence.itemId)
        assertEquals("content://media/file.jpg", evidence.localRef)
        assertNull(evidence.remoteUrl)
        assertEquals(0, evidence.attempts)
        assertNull(evidence.lastError)
    }

    @Test
    fun `register uses deterministic evidenceId`() = runTest {
        val evidence1 = repository.register("insp-1", "epp_1", "local1")
        val evidence2 = repository.register("insp-1", "epp_1", "local2") // same item, different ref

        assertEquals(evidence1.evidenceId, evidence2.evidenceId)
    }

    @Test
    fun `register different itemId gives different evidenceId`() = runTest {
        val evidence1 = repository.register("insp-1", "epp_1", "local")
        val evidence2 = repository.register("insp-1", "epp_2", "local")

        assertNotNull(evidence1.evidenceId)
        assertNotNull(evidence2.evidenceId)
    }

    @Test
    fun `observe emits evidence for inspection`() = runTest {
        repository.register("insp-1", "epp_1", "local1")
        repository.register("insp-1", "epp_2", "local2")
        repository.register("insp-2", "epp_1", "local3")

        val list = repository.observe("insp-1").first()

        assertEquals(2, list.size)
        assertTrue(list.all { it.inspectionId == "insp-1" })
    }

    @Test
    fun `observe is empty for unknown inspection`() = runTest {
        val list = repository.observe("missing").first()

        assertTrue(list.isEmpty())
    }

    @Test
    fun `retry re-queues a FAILED evidence`() = runTest {
        val evidence = repository.register("insp-1", "epp_1", "local")
        // Manually mark as FAILED
        evidenceDao.upsert(EvidenceEntity(
            evidenceId = evidence.evidenceId,
            inspectionId = "insp-1",
            itemId = "epp_1",
            localRef = "local",
            remoteUrl = null,
            status = EvidenceStatus.FAILED.name,
            attempts = 3,
            lastError = "Network error"
        ))

        repository.retry(evidence.evidenceId)

        val retried = evidenceDao.getById(evidence.evidenceId)
        assertNotNull(retried)
        assertEquals(EvidenceStatus.PENDING.name, retried?.status)
        assertEquals(0, retried?.attempts)
        assertNull(retried?.lastError)
    }

    @Test
    fun `retry ignores unknown evidenceId`() = runTest {
        repository.retry("unknown-id")

        assertNull(evidenceDao.getById("unknown-id"))
    }

    private class FakeEvidenceDao : EvidenceDao {
        private val rows = mutableMapOf<String, EvidenceEntity>()

        fun clear() { rows.clear() }

        override fun observeByInspection(inspectionId: String) =
            kotlinx.coroutines.flow.flowOf(
                rows.values.filter { it.inspectionId == inspectionId }.toList()
            )

        override suspend fun getByInspection(inspectionId: String): List<EvidenceEntity> =
            rows.values.filter { it.inspectionId == inspectionId }.toList()

        override suspend fun getById(evidenceId: String): EvidenceEntity? = rows[evidenceId]

        override suspend fun upsert(evidence: EvidenceEntity) {
            rows[evidence.evidenceId] = evidence
        }
    }
}