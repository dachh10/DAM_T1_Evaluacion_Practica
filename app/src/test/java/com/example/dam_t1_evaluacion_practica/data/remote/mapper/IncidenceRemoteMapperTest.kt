package com.example.dam_t1_evaluacion_practica.data.remote.mapper

import com.example.dam_t1_evaluacion_practica.domain.model.Incidence
import com.example.dam_t1_evaluacion_practica.domain.model.IncidenceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Design section 6: the incidence remote mapper is pure Kotlin (no Firebase types,
 * no `org.json`) and never lets a `content://` photo reference reach the cloud.
 */
class IncidenceRemoteMapperTest {

    private val incidence = Incidence(
        id = "inc-1",
        inspectionId = "insp-abc-123",
        checklistItemId = "item-7",
        status = IncidenceStatus.OPEN,
        timestamp = 1_700_000_000_000L,
        latitude = 19.4326,
        longitude = -99.1332,
        photoUri = null,
        notes = "Falta protección auditiva"
    )

    @Test
    fun `toMap writes every remote document field`() {
        val map = IncidenceRemoteMapper.toMap(incidence)

        assertEquals("inc-1", map["id"])
        assertEquals("insp-abc-123", map["inspectionId"])
        assertEquals("item-7", map["checklistItemId"])
        assertEquals("OPEN", map["status"])
        assertEquals(1_700_000_000_000L, map["timestamp"])
        assertEquals(1_700_000_000_000L, map["updatedAt"])
        assertEquals(19.4326, map["latitude"] as Double, 0.0001)
        assertEquals(-99.1332, map["longitude"] as Double, 0.0001)
        assertNull(map["photoUri"])
        assertEquals("Falta protección auditiva", map["notes"])
        assertEquals("LOCAL_ONLY", map["syncState"])
        assertFalse(map["deleted"] as Boolean)
    }

    @Test
    fun `toMap substitutes a local content photo reference with null`() {
        val local = incidence.copy(photoUri = "content://com.example.dam_t1_evaluacion_practica.fileprovider/evidence/xyz.jpg")

        val map = IncidenceRemoteMapper.toMap(local)

        assertNull(map["photoUri"])
    }

    @Test
    fun `toMap preserves an already uploaded photo URL`() {
        val remote = incidence.copy(photoUri = "https://res.cloudinary.com/f2ge9jhj/image/upload/inspections/insp-abc/inc-1.jpg")

        val map = IncidenceRemoteMapper.toMap(remote)

        assertEquals("https://res.cloudinary.com/f2ge9jhj/image/upload/inspections/insp-abc/inc-1.jpg", map["photoUri"])
    }

    @Test
    fun `fromMap restores the incidence written by toMap`() {
        val restored = IncidenceRemoteMapper.fromMap(incidence.id, IncidenceRemoteMapper.toMap(incidence))

        assertEquals(incidence, restored)
    }

    @Test
    fun `fromMap applies defaults for missing fields`() {
        val restored = IncidenceRemoteMapper.fromMap("inc-xyz", mapOf())

        assertEquals("inc-xyz", restored.id)
        assertEquals("", restored.inspectionId)
        assertEquals("", restored.checklistItemId)
        assertEquals(IncidenceStatus.OPEN, restored.status)
        assertEquals(0L, restored.timestamp)
        assertNull(restored.latitude)
        assertNull(restored.longitude)
        assertNull(restored.photoUri)
        assertNull(restored.notes)
    }

    @Test
    fun `fromMap ignores unknown keys`() {
        val map = IncidenceRemoteMapper.toMap(incidence) + ("unknownKey" to "ignored")
        val restored = IncidenceRemoteMapper.fromMap(incidence.id, map)

        assertEquals(incidence, restored)
    }
}
