package com.example.dam_t1_evaluacion_practica.data.sync

import com.example.dam_t1_evaluacion_practica.data.local.entity.IncidenceEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.InspectionEntity
import com.example.dam_t1_evaluacion_practica.domain.model.SyncState
import com.example.dam_t1_evaluacion_practica.domain.validation.SyncConflictPolicy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the remote-to-local mirror (design section 6).
 *
 * The rule is exact: apply a remote snapshot ONLY when the local record is SYNCED
 * and the remote is strictly newer. Dirty local records (LOCAL_ONLY, PENDING, FAILED)
 * ignore remote changes entirely until their push is acknowledged.
 */
class CloudMirrorTest {

    private val inspectionDao = FakeInspectionDao()
    private val incidenceDao = FakeIncidenceDao()
    private val remote = FakeRemoteDataSource()
    private val policy = SyncConflictPolicy
    private val mirror = CloudMirror(inspectionDao, incidenceDao, remote, policy)

    @Test
    fun `mirror creates a local copy when the inspection does not exist`() = runTest {
        remote.remoteInspection = inspection(syncState = SyncState.SYNCED, updatedAt = 5_000L)

        mirror.mirrorInspection("insp-1")

        val local = inspectionDao.find("insp-1")
        assertNotNull(local)
        assertEquals(SyncState.SYNCED.name, local?.syncState)
        assertEquals("remote-company", local?.companyName)
    }

    @Test
    fun `mirror applies remote when local is SYNCED and remote is newer`() = runTest {
        inspectionDao.seed(inspection(syncState = SyncState.SYNCED, updatedAt = 3_000L, companyName = "local-company"))
        remote.remoteInspection = inspection(syncState = SyncState.SYNCED, updatedAt = 5_000L, companyName = "remote-company")

        mirror.mirrorInspection("insp-1")

        val local = inspectionDao.find("insp-1")
        assertEquals(SyncState.SYNCED.name, local?.syncState)
        assertEquals("remote-company", local?.companyName)
    }

    @Test
    fun `mirror ignores remote when local is LOCAL_ONLY`() = runTest {
        inspectionDao.seed(inspection(syncState = SyncState.LOCAL_ONLY, updatedAt = 3_000L, companyName = "local-company"))
        remote.remoteInspection = inspection(syncState = SyncState.SYNCED, updatedAt = 5_000L, companyName = "remote-company")

        mirror.mirrorInspection("insp-1")

        val local = inspectionDao.find("insp-1")
        assertEquals(SyncState.LOCAL_ONLY.name, local?.syncState)
        assertEquals("local-company", local?.companyName)
    }

    @Test
    fun `mirror ignores remote when local is PENDING`() = runTest {
        inspectionDao.seed(inspection(syncState = SyncState.PENDING, updatedAt = 3_000L, companyName = "local-company"))
        remote.remoteInspection = inspection(syncState = SyncState.SYNCED, updatedAt = 5_000L, companyName = "remote-company")

        mirror.mirrorInspection("insp-1")

        assertEquals(SyncState.PENDING.name, inspectionDao.find("insp-1")?.syncState)
        assertEquals("local-company", inspectionDao.find("insp-1")?.companyName)
    }

    @Test
    fun `mirror ignores remote when local is FAILED`() = runTest {
        inspectionDao.seed(inspection(syncState = SyncState.FAILED, updatedAt = 3_000L, companyName = "local-company"))
        remote.remoteInspection = inspection(syncState = SyncState.SYNCED, updatedAt = 5_000L, companyName = "remote-company")

        mirror.mirrorInspection("insp-1")

        assertEquals(SyncState.FAILED.name, inspectionDao.find("insp-1")?.syncState)
        assertEquals("local-company", inspectionDao.find("insp-1")?.companyName)
    }

    @Test
    fun `mirror ignores remote when remote is not newer`() = runTest {
        inspectionDao.seed(inspection(syncState = SyncState.SYNCED, updatedAt = 7_000L, companyName = "local-company"))
        remote.remoteInspection = inspection(syncState = SyncState.SYNCED, updatedAt = 5_000L, companyName = "remote-company")

        mirror.mirrorInspection("insp-1")

        assertEquals(SyncState.SYNCED.name, inspectionDao.find("insp-1")?.syncState)
        assertEquals("local-company", inspectionDao.find("insp-1")?.companyName)
    }

    @Test
    fun `mirror ignores remote when remote is missing (null)`() = runTest {
        inspectionDao.seed(inspection(syncState = SyncState.SYNCED, updatedAt = 3_000L, companyName = "local-company"))
        remote.remoteInspection = null

        mirror.mirrorInspection("insp-1")

        assertEquals("local-company", inspectionDao.find("insp-1")?.companyName)
    }

    @Test
    fun `mirror deletes locally only when local is SYNCED and remote is REMOVED`() = runTest {
        inspectionDao.seed(inspection(syncState = SyncState.SYNCED))
        remote.remoteInspection = null // simulates REMOVED

        mirror.handleRemoved("insp-1")

        val local = inspectionDao.find("insp-1")
        assertNotNull(local)
        assertTrue(local?.deleted ?: false)
        assertEquals(SyncState.PENDING.name, local?.syncState)
    }

    @Test
    fun `mirror ignores REMOVED when local is not SYNCED`() = runTest {
        inspectionDao.seed(inspection(syncState = SyncState.LOCAL_ONLY))
        remote.remoteInspection = null

        mirror.handleRemoved("insp-1")

        val local = inspectionDao.find("insp-1")
        assertNotNull(local)
        assertFalse(local?.deleted ?: true)
        assertEquals(SyncState.LOCAL_ONLY.name, local?.syncState)
    }

    private fun inspection(
        inspectionId: String = "insp-1",
        syncState: SyncState = SyncState.SYNCED,
        updatedAt: Long = 5_000L,
        companyName: String = "remote-company"
    ) = InspectionEntity(
        inspectionId = inspectionId,
        folio = "FOL-1",
        userEmail = "test@x",
        ownerUid = "uid-1",
        companyName = companyName,
        zoneName = "Zona 1",
        auditType = "Interna",
        inspectorName = "Diego",
        inspectorCedula = "123",
        timestamp = 1_000L,
        updatedAt = updatedAt,
        lastSyncedAt = updatedAt,
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
        deleted = false
    )
}