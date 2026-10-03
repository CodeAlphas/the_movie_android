package com.codealphas.themovie.data.remote

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class RetryInterceptorTest {
    private lateinit var server: MockWebServer
    private val delays = mutableListOf<Long>()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        delays.clear()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `지터가 0일 때 503이 두 번 온 뒤 성공하면 1초와 2초를 기다려 성공을 반환해야 한다`() {
        enqueue(SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE, OK)

        val code = execute(random = { ZERO })

        assertEquals(OK, code)
        assertEquals(MAX_ATTEMPTS, server.requestCount)
        assertEquals(listOf(FIRST_DELAY_MS, SECOND_DELAY_MS), delays)
    }

    @Test
    fun `지터가 최대일 때 503이 두 번 온 뒤 성공하면 1초 반과 2초 반을 기다려 성공을 반환해야 한다`() {
        enqueue(SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE, OK)

        val code = execute(random = { NEAR_ONE })

        assertEquals(OK, code)
        assertEquals(MAX_ATTEMPTS, server.requestCount)
        assertEquals(
            listOf(FIRST_DELAY_MS + JITTER_MS, SECOND_DELAY_MS + JITTER_MS),
            delays,
        )
    }

    @Test
    fun `재시도 대상이 아닌 상태 코드를 받으면 다시 보내지 않고 그 응답을 반환해야 한다`() {
        listOf(OK, NOT_FOUND, UNAUTHORIZED, NOT_IMPLEMENTED, HTTP_VERSION_NOT_SUPPORTED).forEach { status ->
            enqueue(status)
            assertEquals(status, execute())
        }
        assertEquals(NON_TRANSIENT_STATUS_COUNT, server.requestCount)
        assertTrue(delays.isEmpty())
    }

    @Test
    fun `429나 500, 502, 504를 받은 뒤 성공하면 1초를 기다려 성공을 반환해야 한다`() {
        listOf(TOO_MANY_REQUESTS, INTERNAL_SERVER_ERROR, BAD_GATEWAY, GATEWAY_TIMEOUT).forEach { status ->
            val requestsBefore = server.requestCount
            enqueue(status, OK)
            assertEquals(OK, execute())
            assertEquals(requestsBefore + 2, server.requestCount)
        }
        assertEquals(
            listOf(FIRST_DELAY_MS, FIRST_DELAY_MS, FIRST_DELAY_MS, FIRST_DELAY_MS),
            delays,
        )
    }

    @Test
    fun `Retry-After 없는 408을 받으면 OkHttp가 한 번 더 보낸 뒤에만 재시도해야 한다`() {
        // OkHttp는 Retry-After 없는 408을 받으면 RetryInterceptor에 응답을 넘기기 전에 스스로 한 번 다시 보내므로,
        // 다시 보낸 요청이 200이면 RetryInterceptor 대기 없이 성공하는지 확인
        enqueue(REQUEST_TIMEOUT, OK)
        assertEquals(OK, execute())
        assertEquals(2, server.requestCount)
        assertTrue(delays.isEmpty())

        delays.clear()
        val requestsBefore = server.requestCount
        enqueue(REQUEST_TIMEOUT, REQUEST_TIMEOUT, OK)

        assertEquals(OK, execute())
        assertEquals(requestsBefore + MAX_ATTEMPTS, server.requestCount)
        assertEquals(listOf(FIRST_DELAY_MS), delays)
    }

    @Test
    fun `GET과 HEAD가 아닌 요청이 503을 받으면 다시 보내지 않고 503을 반환해야 한다`() {
        listOf("POST", "PUT", "DELETE", "PATCH").forEach { method ->
            enqueue(SERVICE_UNAVAILABLE)
            assertEquals(SERVICE_UNAVAILABLE, execute(method = method))
            assertEquals(method, server.takeRequest().method)
        }
        assertTrue(delays.isEmpty())
    }

    @Test
    fun `HEAD 요청이 503을 받은 뒤 성공하면 1초를 기다려 HEAD로 다시 보내 성공을 반환해야 한다`() {
        enqueue(SERVICE_UNAVAILABLE, OK)

        val code = execute(method = "HEAD")

        assertEquals(OK, code)
        assertEquals(2, server.requestCount)
        assertEquals(listOf(FIRST_DELAY_MS), delays)
        assertEquals(listOf("HEAD", "HEAD"), recordedMethods())
    }

    @Test
    fun `Retry-After가 5초 이내면 지터 없이 그 초만큼 기다려 성공을 반환해야 한다`() {
        listOf(0, 2, MAX_RETRY_AFTER_SECONDS).forEach { seconds ->
            server.enqueue(retryAfter(TOO_MANY_REQUESTS, seconds.toString()))
            server.enqueue(statusResponse(OK))
            assertEquals(OK, execute(random = { NEAR_ONE }))
        }
        assertEquals(
            listOf(0L, TWO_SECONDS_MS, MAX_RETRY_AFTER_SECONDS * MILLIS_PER_SECOND),
            delays,
        )
    }

    @Test
    fun `Retry-After가 5초를 넘거나 날짜 형식이면 기다리지 않고 그 응답을 반환해야 한다`() {
        server.enqueue(retryAfter(TOO_MANY_REQUESTS, "6"))
        server.enqueue(retryAfter(TOO_MANY_REQUESTS, RETRY_AFTER_HTTP_DATE))

        assertEquals(TOO_MANY_REQUESTS, execute())
        assertEquals(TOO_MANY_REQUESTS, execute())
        assertEquals(2, server.requestCount)
        assertTrue(delays.isEmpty())
    }

    @Test
    fun `503이 세 번 연속 오면 1초와 2초를 기다려 재시도한 뒤 마지막 503을 반환해야 한다`() {
        enqueue(SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)

        val code = execute()

        assertEquals(SERVICE_UNAVAILABLE, code)
        assertEquals(MAX_ATTEMPTS, server.requestCount)
        assertEquals(listOf(FIRST_DELAY_MS, SECOND_DELAY_MS), delays)
    }

    @Test
    fun `연결이 시작부터 끊기면 기다리거나 다시 요청하지 않고 입출력 오류를 던져야 한다`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertThrows(IOException::class.java) {
            execute()
        }
        assertTrue(delays.isEmpty())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `재시도 대기 중에 호출이 취소되면 다시 요청하지 않고 취소 오류를 던져야 한다`() {
        enqueue(SERVICE_UNAVAILABLE, OK)
        lateinit var call: Call
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(
                    RetryInterceptor(
                        sleeper = {
                            delays += it
                            call.cancel()
                        },
                        random = { ZERO },
                    ),
                ).build()
        call = client.newCall(request("GET"))

        val thrown =
            assertThrows(IOException::class.java) {
                call.execute()
            }

        assertEquals("Canceled", thrown.message)
        assertEquals(listOf(FIRST_DELAY_MS), delays)
        assertEquals(1, server.requestCount)
    }

    private fun execute(
        method: String = "GET",
        random: () -> Double = { ZERO },
    ): Int {
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(
                    RetryInterceptor(
                        sleeper = { delays += it },
                        random = random,
                    ),
                ).build()
        return client.newCall(request(method)).execute().use { response ->
            response.code
        }
    }

    private fun request(method: String): Request {
        val builder = Request.Builder().url(server.url("/"))
        return when (method) {
            "GET" -> builder.get().build()
            "HEAD" -> builder.head().build()
            "DELETE" -> builder.delete().build()
            else -> builder.method(method, ByteArray(0).toRequestBody()).build()
        }
    }

    private fun recordedMethods(): List<String> = List(server.requestCount) { server.takeRequest().method.orEmpty() }

    private fun enqueue(vararg statuses: Int) {
        statuses.forEach { status -> server.enqueue(statusResponse(status)) }
    }

    private fun statusResponse(status: Int): MockResponse = MockResponse().setResponseCode(status)

    private fun retryAfter(
        status: Int,
        value: String,
    ): MockResponse = statusResponse(status).setHeader("Retry-After", value)

    private companion object {
        const val ZERO = 0.0
        const val NEAR_ONE = 0.999
        const val OK = 200
        const val REQUEST_TIMEOUT = 408
        const val TOO_MANY_REQUESTS = 429
        const val UNAUTHORIZED = 401
        const val NOT_FOUND = 404
        const val INTERNAL_SERVER_ERROR = 500
        const val NOT_IMPLEMENTED = 501
        const val BAD_GATEWAY = 502
        const val SERVICE_UNAVAILABLE = 503
        const val GATEWAY_TIMEOUT = 504
        const val HTTP_VERSION_NOT_SUPPORTED = 505
        const val NON_TRANSIENT_STATUS_COUNT = 5
        const val MAX_ATTEMPTS = 3
        const val MAX_RETRY_AFTER_SECONDS = 5L
        const val MILLIS_PER_SECOND = 1_000L
        const val FIRST_DELAY_MS = 1_000L
        const val SECOND_DELAY_MS = 2_000L
        const val TWO_SECONDS_MS = 2_000L
        const val JITTER_MS = 500L
        const val RETRY_AFTER_HTTP_DATE = "Wed, 21 Oct 2015 07:28:00 GMT"
    }
}
