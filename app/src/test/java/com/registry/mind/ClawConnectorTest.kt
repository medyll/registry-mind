package com.registry.mind

import com.registry.mind.data.Header
import com.registry.mind.data.NavigationMeta
import com.registry.mind.data.Payload
import com.registry.mind.data.RegistryPacket
import com.registry.mind.network.ClawApiService
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Tests ClawApiService contract via MockWebServer.
 * Does NOT test ClawConnector singleton directly (requires Android context for SettingsManager).
 * Tests the underlying Retrofit service layer.
 */
class ClawConnectorTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ClawApiService

    private val testPacket = RegistryPacket(
        header = Header(
            protocol = "registry-mind-v1",
            device = "Oppo_Find_X9_Native",
            timestamp = "2026-04-22T12:00:00Z",
            authToken = "test-token-abc123"
        ),
        payload = Payload(
            imageData = "base64imagedata",
            ocrContent = "Some extracted text",
            sourceApp = "com.android.chrome",
            aiGuess = "work_meeting"
        ),
        navigationMeta = NavigationMeta(
            role = "registry_sensor",
            sessionState = "active_chat",
            tag = "Work"
        )
    )

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        val client = OkHttpClient.Builder().build()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ClawApiService::class.java)
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `sendPacket returns success on HTTP 200`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        val response = api.sendPacket("test-token-abc123", testPacket)

        assertTrue("Expected 200 success", response.isSuccessful)
    }

    @Test
    fun `sendPacket returns failure on HTTP 500`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val response = api.sendPacket("test-token-abc123", testPacket)

        assertFalse("Expected 500 failure", response.isSuccessful)
        assertEquals(500, response.code())
    }

    @Test
    fun `sendPacket returns failure on HTTP 401`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))

        val response = api.sendPacket("bad-token", testPacket)

        assertFalse("Expected 401 failure", response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun `sendPacket sends correct auth header`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        api.sendPacket("my-secret-token", testPacket)

        val recorded = server.takeRequest()
        assertEquals("my-secret-token", recorded.getHeader("X-Registry-Auth"))
    }

    @Test
    fun `sendPacket hits correct endpoint`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        api.sendPacket("token", testPacket)

        val recorded = server.takeRequest()
        assertEquals("/api/v1/capture", recorded.path)
        assertEquals("POST", recorded.method)
    }

    @Test
    fun `sendPacket serializes NavigationMeta tag field`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        api.sendPacket("token", testPacket)

        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertTrue("Body should contain tag field", body.contains("\"tag\":\"Work\""))
        assertTrue("Body should contain session_state", body.contains("\"session_state\":\"active_chat\""))
    }
}
