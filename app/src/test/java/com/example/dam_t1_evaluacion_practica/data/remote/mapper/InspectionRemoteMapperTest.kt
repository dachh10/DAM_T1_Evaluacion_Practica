package com.example.dam_t1_evaluacion_practica.data.remote.mapper

import com.example.dam_t1_evaluacion_practica.domain.model.IncidenceStatus
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.domain.model.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Design section 6: the inspection remote mapper is pure Kotlin (no Firebase types,
 * no `org.json`) and the produced document never carries a `content://` photo reference.
 */
class InspectionRemoteMapperTest {

    private val record = InspectionSessionRecord(
        folio = "FOL-2026-001",
        inspectionId = "insp-abc-123",
        userEmail = "inspector@seguridad.mx",
        companyName = "Construcciones del Norte",
        zoneName = "Zona Centro",
        auditType = "Seguridad industrial",
        inspectorName = "Ing. Laura Gomez",
        inspectorCedula = "STPS-9988",
        timestamp = 1_700_000_000_000L,
        overallScore = 92,
        criticalStop = false,
        reinspectionDue = false,
        scheduledReinspectionAt = null,
        modules = emptyList(),
        findings = emptyList(),
        normCodes = listOf("NOM-017-STPS"),
        latitude = 19.4326,
        longitude = -99.1332,
        status = IncidenceStatus.OPEN,
        ownerUid = "uid-123",
        syncState = SyncState.LOCAL_ONLY,
        updatedAt = 1_700_000_000_000L
    )

    @Test
    fun `toMap writes every remote document field`() {
        val map = InspectionRemoteMapper.toMap(record)

        assertEquals("insp-abc-123", map["inspectionId"])
        assertEquals("FOL-2026-001", map["folio"])
        assertEquals("uid-123", map["ownerUid"])
        assertEquals("inspector@seguridad.mx", map["userEmail"])
        assertEquals("Construcciones del Norte", map["companyName"])
        assertEquals("Zona Centro", map["zoneName"])
        assertEquals("Seguridad industrial", map["auditType"])
        assertEquals("Ing. Laura Gomez", map["inspectorName"])
        assertEquals("STPS-9988", map["inspectorCedula"])
        assertEquals(1_700_000_000_000L, map["timestamp"])
        assertEquals(1_700_000_000_000L, map["updatedAt"])
        assertNull(map["lastSyncedAt"])
        assertEquals(92, map["overallScore"])
        assertFalse(map["criticalStop"] as Boolean)
        assertFalse(map["reinspectionDue"] as Boolean)
        assertNull(map["scheduledReinspectionAt"])
        assertEquals("OPEN", map["status"])
        assertEquals(19.4326, map["latitude"] as Double, 0.0001)
        assertEquals(-99.1332, map["longitude"] as Double, 0.0001)
        assertEquals("LOCAL_ONLY", map["syncState"])
        assertFalse(map["deleted"] as Boolean)
    }

    @Test
    fun `toMap produces JSON strings for modules, findings and normCodes`() {
        val map = InspectionRemoteMapper.toMap(record)

        assertEquals("[]", map["modulesJson"])
        assertEquals("[]", map["findingsJson"])
        assertEquals("[\"NOM-017-STPS\"]", map["normCodesJson"])
    }

    @Test
    fun `toMap substitutes a local content photo reference with null`() {
        val finding = com.example.dam_t1_evaluacion_practica.domain.model.SessionFinding(
            itemId = "item-1",
            itemDescription = "Guardapolvos",
            normCode = "NOM-017",
            criticalityLevel = com.example.dam_t1_evaluacion_practica.domain.model.CriticalityLevel.MEDIUM,
            isNA = false,
            notes = null,
            photoUri = "content://com.example.dam_t1_evaluacion_practica.fileprovider/evidence/abc.jpg",
            timestamp = 1_700_000_000_000L
        )
        val recordWithLocalPhoto = record.copy(findings = listOf(finding))

        val map = InspectionRemoteMapper.toMap(recordWithLocalPhoto)
        val json = map["findingsJson"] as String

        assertTrue(json.contains("\"photoUri\":null"))
    }

    @Test
    fun `toMap preserves an already uploaded photo URL`() {
        val finding = com.example.dam_t1_evaluacion_practica.domain.model.SessionFinding(
            itemId = "item-1",
            itemDescription = "Guardapolvos",
            normCode = "NOM-017",
            criticalityLevel = com.example.dam_t1_evaluacion_practica.domain.model.CriticalityLevel.MEDIUM,
            isNA = false,
            notes = null,
            photoUri = "https://res.cloudinary.com/f2ge9jhj/image/upload/inspections/insp-abc/item-1.jpg",
            timestamp = 1_700_000_000_000L
        )
        val recordWithRemotePhoto = record.copy(findings = listOf(finding))

        val map = InspectionRemoteMapper.toMap(recordWithRemotePhoto)
        val json = map["findingsJson"] as String

        assertTrue(json.contains("https://res.cloudinary.com/f2ge9jhj/image/upload/"))
    }

    @Test
    fun `fromMap restores the record written by toMap`() {
        val restored = InspectionRemoteMapper.fromMap(record.inspectionId, InspectionRemoteMapper.toMap(record))

        assertEquals(record, restored)
    }

    @Test
    fun `fromMap applies defaults for missing fields`() {
        val restored = InspectionRemoteMapper.fromMap("insp-xyz", mapOf())

        assertEquals("insp-xyz", restored.inspectionId)
        assertEquals("", restored.folio)
        assertEquals("", restored.userEmail)
        assertEquals("", restored.ownerUid)
        assertEquals(IncidenceStatus.OPEN, restored.status)
        assertEquals(SyncState.LOCAL_ONLY, restored.syncState)
        assertEquals(0L, restored.timestamp)
        assertEquals(0L, restored.updatedAt)
        assertEquals(0, restored.overallScore)
        assertTrue(restored.modules.isEmpty())
        assertTrue(restored.findings.isEmpty())
        assertTrue(restored.normCodes.isEmpty())
    }

    @Test
    fun `fromMap ignores unknown keys`() {
        val map = InspectionRemoteMapper.toMap(record) + ("unknownKey" to "ignored")
        val restored = InspectionRemoteMapper.fromMap(record.inspectionId, map)

        assertEquals(record, restored)
    }

    @Test
    fun `fromMap parses nested modules, findings and normCodes`() {
        val map = InspectionRemoteMapper.toMap(record)
        val restored = InspectionRemoteMapper.fromMap(record.inspectionId, map)

        assertEquals(listOf("NOM-017-STPS"), restored.normCodes)
    }
}
