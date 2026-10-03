package com.codealphas.themovie.data.map.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class KakaoApiKeyInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `요청을 보내면 Authorization 헤더에 KakaoAK 접두사와 키를 붙여야 한다`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder().addInterceptor(KakaoApiKeyInterceptor("rest-key")).build()

        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        assertEquals("KakaoAK rest-key", server.takeRequest().getHeader("Authorization"))
    }
}
