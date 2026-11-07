package com.example.dam_t1_evaluacion_practica.util

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Tests for [RemoteImageLoader] (design section 8).
 *
 * Contract:
 * - Loads image from HTTPS URL
 * - Returns byte array on success
 * - Returns null on 4xx/5xx, timeout, or network error
 * - Respects 2 MiB read cap
 * - 10 second timeout
 */
class RemoteImageLoaderTest {

    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `load returns bytes for successful 200 response`() = runTest {
        val imageBytes = "fake-jpeg-data".toByteArray()
        server.enqueue(MockResponse().setResponseCode(200).setBody(String(imageBytes)))

        val url = server.url("/test.jpg").toString()
        com.example.dam_t1_evaluacion_practica.util.RemoteImageLoader.testMode = true
        val result = com.example.dam_t1_evaluacion_practica.util.RemoteImageLoader.load(url)

        assertNotNull(result)
        assertTrue(result!!.contentEquals(imageBytes))
    }

    @Test
    fun `load returns null for 404`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        val url = server.url("/missing.jpg").toString()
        com.example.dam_t1_evaluacion_practica.util.RemoteImageLoader.testMode = true
        val result = com.example.dam_t1_evaluacion_practica.util.RemoteImageLoader.load(url)

        assertNull(result)
    }

    @Test
    fun `load returns null for 500`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val url = server.url("/error.jpg").toString()
        com.example.dam_t1_evaluacion_practica.util.RemoteImageLoader.testMode = true
        val result = com.example.dam_t1_evaluacion_practica.util.RemoteImageLoader.load(url)

        assertNull(result)
    }

    @Test
    fun `load returns null for empty body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))

        val url = server.url("/empty.jpg").toString()
        com.example.dam_t1_evaluacion_practica.util.RemoteImageLoader.testMode = true
        val result = com.example.dam_t1_evaluacion_practica.util.RemoteImageLoader.load(url)

        assertNull(result)
    }

    @Test
    fun `load enforces 2 MiB cap`() = runTest {
        val largeBody = ByteArray(3 * 1024 * 1024) // 3 MiB
        server.enqueue(MockResponse().setResponseCode(200).setBody(String(largeBody)))

        val url = server.url("/large.jpg").toString()
        com.example.dam_t1_evaluacion_practica.util.RemoteImageLoader.testMode = true
        val result = com.example.dam_t1_evaluacion_practica.util.RemoteImageLoader.load(url)

        assertNull("should reject > 2 MiB", result)
    }

    @Test
    fun `load allows exactly 2 MiB`() = runTest {
        val exactBody = ByteArray(2 * 1024 * 1024) // exactly 2 MiB
        server.enqueue(MockResponse().setResponseCode(200).setBody(String(exactBody)))

        val url = server.url("/exact.jpg").toString()
        com.example.dam_t1_evaluacion_practica.util.RemoteImageLoader.testMode = true
        val result = com.example.dam_t1_evaluacion_practica.util.RemoteImageLoader.load(url)

        assertNotNull(result)
        assertEquals(2 * 1024 * 1024, result!!.size)
    }
}