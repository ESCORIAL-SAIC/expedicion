package com.expedicion.app.data.config

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Ejercita ApiProbe contra un servidor HTTP real (MockWebServer) devolviendo los mismos bodies
 * que produce `api/src/app.ts`, en vez de mockear OkHttp.
 */
class ApiProbeTest {

    private lateinit var server: MockWebServer
    private val probe = ApiProbe()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun baseUrl() = server.url("/").toString()

    private fun dispatch(handler: (String) -> MockResponse) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = handler(request.path ?: "")
        }
    }

    @Test
    fun `health ready ok devuelve Ok con la version del API`() = runTest {
        dispatch { path ->
            when (path) {
                "/health/ready" -> MockResponse().setResponseCode(200)
                    .setBody("""{"status":"ok","checks":{"postgres":"ok","mssql":"ok"}}""")
                "/version" -> MockResponse().setResponseCode(200).setBody("""{"version":"1.4.0"}""")
                else -> MockResponse().setResponseCode(404)
            }
        }

        val result = probe.probe(baseUrl())

        assertEquals(ApiProbeResult.Ok("1.4.0"), result)
    }

    @Test
    fun `en modo dev el API reporta version dev y sigue siendo Ok`() = runTest {
        dispatch { path ->
            when (path) {
                "/health/ready" -> MockResponse().setResponseCode(200)
                    .setBody("""{"status":"ok","checks":{"postgres":"ok","mssql":"ok"}}""")
                "/version" -> MockResponse().setResponseCode(200).setBody("""{"version":"dev"}""")
                else -> MockResponse().setResponseCode(404)
            }
        }

        assertEquals(ApiProbeResult.Ok("dev"), probe.probe(baseUrl()))
    }

    @Test
    fun `health ready 503 identifica cual base no responde`() = runTest {
        dispatch { path ->
            if (path == "/health/ready") {
                MockResponse().setResponseCode(503)
                    .setBody("""{"status":"error","checks":{"postgres":"ok","mssql":"error"}}""")
            } else {
                MockResponse().setResponseCode(404)
            }
        }

        val result = probe.probe(baseUrl())

        assertEquals(ApiProbeResult.DbUnavailable(postgresOk = true, mssqlOk = false), result)
    }

    @Test
    fun `un servidor que responde otra cosa da NotHealthy`() = runTest {
        dispatch { MockResponse().setResponseCode(404).setBody("Not Found") }

        val result = probe.probe(baseUrl())

        assertTrue(result is ApiProbeResult.NotHealthy)
        assertTrue((result as ApiProbeResult.NotHealthy).detalle.contains("404"))
    }

    @Test
    fun `un puerto cerrado da Unreachable`() = runTest {
        val url = baseUrl()
        server.shutdown()

        assertEquals(ApiProbeResult.Unreachable, probe.probe(url))
    }

    @Test
    fun `si version falla pero health ready esta ok la URL sigue siendo valida`() = runTest {
        dispatch { path ->
            if (path == "/health/ready") {
                MockResponse().setResponseCode(200)
                    .setBody("""{"status":"ok","checks":{"postgres":"ok","mssql":"ok"}}""")
            } else {
                MockResponse().setResponseCode(500)
            }
        }

        val result = probe.probe(baseUrl())

        assertEquals(ApiProbeResult.Ok(ApiProbe.VERSION_DESCONOCIDA), result)
    }
}
