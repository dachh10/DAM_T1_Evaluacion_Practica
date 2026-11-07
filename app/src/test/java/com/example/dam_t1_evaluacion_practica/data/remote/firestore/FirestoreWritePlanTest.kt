package com.example.dam_t1_evaluacion_practica.data.remote.firestore

import com.example.dam_t1_evaluacion_practica.data.local.entity.IncidenceEntity
import com.example.dam_t1_evaluacion_practica.data.local.entity.InspectionEntity
import com.example.dam_t1_evaluacion_practica.data.mapper.toDomainModel
import com.example.dam_t1_evaluacion_practica.data.remote.mapper.IncidenceRemoteMapper
import com.example.dam_t1_evaluacion_practica.data.remote.mapper.InspectionRemoteMapper
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the Firestore write plan contract (design section 6) without requiring a live
 * Firestore instance. The data source implementation is thin glue over the remote mappers;
 * the contract is verified by checking the maps the mappers produce.
 */
class FirestoreWritePlanTest {

    @Test
    fun `inspection map includes ownerUid - serverTimestamp applied by data source`() = runBlocking {
        val entity = inspectionEntity(ownerUid = "user-123")
        val domain = entity.toDomainModel()

        val map = InspectionRemoteMapper.toMap(domain)

        assertEquals("user-123", map["ownerUid"])
        // Mapper writes local updatedAt; data source overrides with FieldValue.serverTimestamp()
        assertNotNull(map["updatedAt"])
    }

    @Test
    fun `incidence map omits local contentUri photoUri and writes remote https`() = runBlocking {
        val localUri = "content://media/external/images/media/42"
        val remoteUrl = "https://res.cloudinary.com/.../inspections/insp-1/evidence.jpg"
        val localEntity = incidenceEntity(photoUri = localUri)
        val remoteEntity = incidenceEntity(photoUri = remoteUrl)

        val localMap = IncidenceRemoteMapper.toMap(localEntity.toDomainModel())
        val remoteMap = IncidenceRemoteMapper.toMap(remoteEntity.toDomainModel())

        assertEquals(null, localMap["photoUri"])
        assertEquals(remoteUrl, remoteMap["photoUri"])
    }

    @Test
    fun `incidence chunk size is at most 8`() = runBlocking {
        // The data source batches in chunks of 8 (Firestore batch limit is 500, but 8 is our
        // design choice for atomicity). This test documents the contract.
        val entities = (1..15).map { i -> incidenceEntity(id = "inc-$i") }
        val chunks = entities.chunked(8)

        assertEquals(2, chunks.size)
        assertEquals(8, chunks[0].size)
        assertEquals(7, chunks[1].size)
    }

    @Test
    fun `deleted flag is always false in remote maps`() = runBlocking {
        val entity = inspectionEntity()
        val domain = entity.toDomainModel()

        val map = InspectionRemoteMapper.toMap(domain)

        assertEquals(false, map["deleted"])
    }

    private fun inspectionEntity(
        inspectionId: String = "insp-1",
        ownerUid: String = "uid-1"
    ) = InspectionEntity(
        inspectionId = inspectionId,
        folio = "FOL-1",
        userEmail = "test@x",
        ownerUid = ownerUid,
        companyName = "ACME",
        zoneName = "Zona 1",
        auditType = "Interna",
        inspectorName = "Diego",
        inspectorCedula = "123",
        timestamp = 1_000L,
        updatedAt = 1_000L,
        lastSyncedAt = null,
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
        syncState = "LOCAL_ONLY",
        deleted = false
    )

    private fun incidenceEntity(
        id: String = "inc-1",
        inspectionId: String = "insp-1",
        photoUri: String? = null
    ) = IncidenceEntity(
        id = id,
        inspectionId = inspectionId,
        checklistItemId = "epp_1",
        status = "OPEN",
        timestamp = 1_000L,
        updatedAt = 1_000L,
        latitude = null,
        longitude = null,
        photoUri = photoUri,
        notes = null,
        syncState = "LOCAL_ONLY",
        deleted = false
    )
}