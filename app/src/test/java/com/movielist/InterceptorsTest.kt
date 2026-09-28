package com.movielist

import com.movielist.tmdb.network.ApiKeyInterceptor
import com.movielist.tmdb.network.MissingApiKeyException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class InterceptorsTest {

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

    private fun get(client: OkHttpClient, path: String) =
        client.newCall(Request.Builder().url(server.url(path)).build()).execute()

    @Test
    fun missingApiKey_failsBeforeAnythingIsSent() {
        val client = OkHttpClient.Builder().addInterceptor(ApiKeyInterceptor()).build()

        assertThrows(MissingApiKeyException::class.java) { get(client, "/3/movie/1?api_key=") }
        assertThrows(MissingApiKeyException::class.java) { get(client, "/3/movie/1") }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun presentApiKey_isPassedThrough() {
        server.enqueue(MockResponse().setBody("{}"))
        val client = OkHttpClient.Builder().addInterceptor(ApiKeyInterceptor()).build()

        get(client, "/3/movie/1?api_key=abc").use { response ->
            assertEquals(200, response.code)
        }
        assertEquals(1, server.requestCount)
    }
}
