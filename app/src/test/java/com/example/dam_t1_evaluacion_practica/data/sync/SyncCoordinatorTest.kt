package com.example.dam_t1_evaluacion_practica.data.sync

import com.example.dam_t1_evaluacion_practica.data.local.entity.IncidenceEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.InspectionEntity
import com.example.dam_t1_evaluacion_practica.domain.model.SyncState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the SYNC state machine (design section 6).
 *
 * `SyncCoordinator` is the single writer of `syncState` in Room, so these are the tests that
 * decide when a record becomes PENDING, SYNCED or FAILED, and in which order Firestore sees
 * the writes.
 */
class SyncCoordinatorTest {

    private val inspectionDao = FakeInspectionDao()
    private val incidenceDao = FakeIncidenceDao()
    private val remote = FakeRemoteDataSource()
    private val scheduler = RecordingSyncScheduler()
    private val coordinator = SyncCoordinator(inspectionDao, incidenceDao, remote, scheduler)

    @Test
    fun `markPending moves a local-only inspection to pending and queues work`() = runTest {
        inspectionDao.seed(inspection(syncState = SyncState.LOCAL_ONLY))

        coordinator.markPending("insp-1")

        assertEquals(SyncState.PENDING.name, inspectionDao.find("insp-1")?.syncState)
        assertEquals(listOf("insp-1"), scheduler.enqueued)
    }

    @Test
    fun `markPending ignores an unknown inspection`() = runTest {
        coordinator.markPending("missing")

        assertNull(inspectionDao.find("missing"))
        assertTrue(scheduler.enqueued.isEmpty())
    }

    @Test
    fun `syncInspection pushes the parent document before its incidences`() = runTest {
        inspectionDao.seed(inspection(syncState = SyncState.PENDING))
        incidenceDao.seed(incidence("inc-1"))

        coordinator.syncInspection("insp-1")

        assertEquals(listOf("saveInspection", "saveIncidences"), remote.calls)
    }

    @Test
    fun `syncInspection marks the inspection synced and stamps lastSyncedAt`() = runTest {
        inspectionDao.seed(inspection(syncState = SyncState.PENDING))

        coordinator.syncInspection("insp-1")

        val stored = inspectionDao.find("insp-1")
        assertEquals(SyncState.SYNCED.name, stored?.syncState)
        assertNotNull(stored?.lastSyncedAt)
        assertTrue("lastSyncedAt must be positive", (stored?.lastSyncedAt ?: 0L) > 0L)
    }

    @Test
    fun `syncInspection only sends the incidences that belong to it`() = runTest {
        inspectionDao.seed(inspection(syncState = SyncState.PENDING))
        incidenceDao.seed(
            incidence("inc-1", inspectionId = "insp-1"),
            incidence("inc-2", inspectionId = "insp-other")
        )

        coordinator.syncInspection("insp-1")

        assertEquals(listOf("inc-1"), remote.savedIncidences?.second?.map { it.id })
    }

    @Test
    fun `syncInspection pushes the current Room state, not a stale copy`() = runTest {
        inspectionDao.seed(inspection(syncState = SyncState.PENDING, companyName = "old"))
        inspectionDao.updateInspection(
            inspection(syncState = SyncState.PENDING, companyName = "edited-after-enqueue")
        )

        coordinator.syncInspection("insp-1")

        assertEquals("edited-after-enqueue", remote.savedInspections.single().companyName)
    }

    @Test
    fun `syncInspection ignores an unknown inspection`() = runTest {
        coordinator.syncInspection("missing")

        assertTrue(remote.calls.isEmpty())
    }

    @Test
    fun `markFailed moves the inspection to failed`() = runTest {
        inspectionDao.seed(inspection(syncState = SyncState.PENDING))

        coordinator.markFailed("insp-1")

        assertEquals(SyncState.FAILED.name, inspectionDao.find("insp-1")?.syncState)
    }

    @Test
    fun `markFailed ignores an unknown inspection`() = runTest {
        coordinator.markFailed("missing")

        assertNull(inspectionDao.find("missing"))
    }

    private fun inspection(
        inspectionId: String = "insp-1",
        folio: String = "FOL-1",
        ownerUid: String = "uid-1",
        companyName: String = "ACME",
        updatedAt: Long = 1_000L,
        lastSyncedAt: Long? = null,
        syncState: SyncState = SyncState.LOCAL_ONLY,
        deleted: Boolean = false
    ) = InspectionEntity(
        inspectionId = inspectionId,
        folio = folio,
        userEmail = "inspector@seguridad.mx",
        ownerUid = ownerUid,
        companyName = companyName,
        zoneName = "Zona 1",
        auditType = "Interna",
        inspectorName = "Diego",
        inspectorCedula = "12345678",
        timestamp = 1_000L,
        updatedAt = updatedAt,
        lastSyncedAt = lastSyncedAt,
        overallScore = 90,
        criticalStop = false,
        reinspectionDue = false,
        scheduledReinspectionAt = null,
        status = "CONFORME",
        latitude = null,
        longitude = null,
        modulesJson = "[]",
        findingsJson = "[]",
        normCodesJson = "[]",
        syncState = syncState.name,
        deleted = deleted
    )

    private fun incidence(
        id: String,
        inspectionId: String = "insp-1",
        updatedAt: Long = 1_000L,
        syncState: SyncState = SyncState.LOCAL_ONLY
    ) = IncidenceEntity(
        id = id,
        inspectionId = inspectionId,
        checklistItemId = "epp_1",
        status = "OPEN",
        timestamp = 1_000L,
        updatedAt = updatedAt,
        latitude = null,
        longitude = null,
        photoUri = null,
        notes = null,
        syncState = syncState.name,
        deleted = false
    )
}
