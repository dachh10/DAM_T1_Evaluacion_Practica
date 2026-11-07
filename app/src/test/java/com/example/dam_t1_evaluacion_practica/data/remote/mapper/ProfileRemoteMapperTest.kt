package com.example.dam_t1_evaluacion_practica.data.remote.mapper

import com.example.dam_t1_evaluacion_practica.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Design section 5: the profile mapper is pure Kotlin (no Firebase types, no `org.json`)
 * and the produced document never carries a credential.
 */
class ProfileRemoteMapperTest {

    private val profile = User(
        email = "inspector@seguridad.mx",
        name = "Ing. Laura Gomez",
        cedula = "STPS-9988",
        createdAt = 1_700_000_000_000L,
        uid = "uid-123"
    )

    @Test
    fun `toMap writes the remote document fields without any credential`() {
        val map = ProfileRemoteMapper.toMap(profile)

        assertEquals("inspector@seguridad.mx", map["email"])
        assertEquals("Ing. Laura Gomez", map["displayName"])
        assertEquals("STPS-9988", map["cedula"])
        assertEquals(setOf("email", "displayName", "cedula", "createdAt"), map.keys)
        assertFalse(map.containsKey("password"))
        assertFalse(map.containsKey("updatedAt")) // server-owned, never client-supplied
    }

    @Test
    fun `fromMap restores the profile written by toMap`() {
        val restored = ProfileRemoteMapper.fromMap("uid-123", ProfileRemoteMapper.toMap(profile))

        assertEquals(profile, restored)
    }

    @Test
    fun `fromMap applies defaults for missing fields and ignores unknown keys`() {
        val restored = ProfileRemoteMapper.fromMap(
            "uid-456",
            mapOf("cedula" to "STPS-1", "unknown" to "ignored")
        )

        assertEquals("uid-456", restored.uid)
        assertEquals("", restored.email)
        assertEquals("", restored.name)
        assertEquals("STPS-1", restored.cedula)
        assertEquals(0L, restored.createdAt)
    }

    @Test
    fun `fromMap reads a numeric createdAt as Long`() {
        val restored = ProfileRemoteMapper.fromMap("uid-789", mapOf("createdAt" to 1_700_000_000_000L))

        assertEquals(1_700_000_000_000L, restored.createdAt)
    }
}
