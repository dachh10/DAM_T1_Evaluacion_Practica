package com.example.dam_t1_evaluacion_practica.ui.viewmodel

import com.example.dam_t1_evaluacion_practica.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The dashboard, the top bar and the module header greet the authenticated inspector, so the
 * display helpers must prefer the session identity and only fall back to the demo profile when
 * there is no session (or the session carries blank fields).
 */
class InspectorProfileTest {

    private val user = User(
        email = "inspector.prueba@seguridad.mx",
        name = "Inspector Prueba",
        cedula = "STPS-AUD-2026-0451",
        uid = "uid-abc"
    )

    @Test
    fun `nameOf returns the authenticated name`() {
        assertEquals("Inspector Prueba", InspectorProfile.nameOf(user))
    }

    @Test
    fun `nameOf falls back to the demo identity without a session`() {
        assertEquals(InspectorProfile.NAME, InspectorProfile.nameOf(null))
    }

    @Test
    fun `nameOf falls back when the session name is blank`() {
        assertEquals(InspectorProfile.NAME, InspectorProfile.nameOf(user.copy(name = "   ")))
    }

    @Test
    fun `cedulaOf returns the authenticated cedula`() {
        assertEquals("STPS-AUD-2026-0451", InspectorProfile.cedulaOf(user))
    }

    @Test
    fun `cedulaOf falls back to the demo cedula without a session`() {
        assertEquals(InspectorProfile.CEDULA, InspectorProfile.cedulaOf(null))
    }

    @Test
    fun `initialsOf derives two letters from the authenticated name`() {
        assertEquals("IP", InspectorProfile.initialsOf(user))
    }

    @Test
    fun `initialsOf skips honorific titles and keeps two names`() {
        assertEquals(
            "DA",
            InspectorProfile.initialsOf(user.copy(name = "Ing. Diego Adrian"))
        )
    }

    @Test
    fun `initialsOf falls back to the demo initials without a session`() {
        assertEquals(InspectorProfile.INITIALS, InspectorProfile.initialsOf(null))
    }
}
