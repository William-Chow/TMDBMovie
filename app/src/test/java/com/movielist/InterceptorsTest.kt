package com.movielist

import com.movielist.tmdb.network.ApiKeyInterceptor
import com.movielist.tmdb.network.CacheHeaderInterceptor
import com.movielist.tmdb.network.MissingApiKeyException
import com.movielist.tmdb.network.StaleCacheFallbackInterceptor
import com.movielist.tmdb.network.isUnsatisfiedCacheOnlyResponse
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
import java.nio.file.Files

class InterceptorsTest {

    private lateinit var server: MockWebServer
    private lateinit var cacheDir: File

    /** Flipped by tests to make every request that needs the network fail, as offline. */
    private var offline = false

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        cacheDir = Files.createTempDirectory("http-cache").toFile()
    }

    @After
    fun tearDown() {
        server.shutdown()
        cacheDir.deleteRecursively()
    }

    /**
     * The app's interceptor stack, except that responses are stale straight
     * away (max-age=0), so every request really tries the network.
     */
    private fun appLikeClient(): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(cacheDir, 1024L * 1024))
        .addInterceptor(ApiKeyInterceptor())
        .addInterceptor(StaleCacheFallbackInterceptor(maxStaleDays = 7))
        .addNetworkInterceptor { chain ->
            if (offline) throw UnknownHostException("offline") else chain.proceed(chain.request())
        }
        .addNetworkInterceptor(CacheHeaderInterceptor(maxAgeSeconds = 0))
        .build()

    private fun request(path: String = "/3/movie/1?api_key=abc", noCache: Boolean = false): Request =
        Request.Builder()
            .url(server.url(path))
            .apply { if (noCache) cacheControl(CacheControl.FORCE_NETWORK) }
            .build()

    private fun OkHttpClient.fetch(request: Request): Response = newCall(request).execute()

    @Test
    fun missingApiKey_failsBeforeAnythingIsSent() {
        val client = OkHttpClient.Builder().addInterceptor(ApiKeyInterceptor()).build()

        assertThrows(MissingApiKeyException::class.java) { client.fetch(request("/3/movie/1?api_key=")) }
        assertThrows(MissingApiKeyException::class.java) { client.fetch(request("/3/movie/1")) }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun presentApiKey_isPassedThrough() {
        server.enqueue(MockResponse().setBody("{}"))
        val client = OkHttpClient.Builder().addInterceptor(ApiKeyInterceptor()).build()

        client.fetch(request()).use { assertEquals(200, it.code) }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun online_theNetworkIsAlwaysTriedFirst() {
        server.enqueue(MockResponse().setBody("v1"))
        server.enqueue(MockResponse().setBody("v2"))
        val client = appLikeClient()

        client.fetch(request()).use { assertEquals("v1", it.body!!.string()) }
        client.fetch(request()).use { assertEquals("v2", it.body!!.string()) }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun offline_servesTheLastCopyFromTheCache() {
        server.enqueue(MockResponse().setBody("v1"))
        val client = appLikeClient()
        client.fetch(request()).use { assertEquals("v1", it.body!!.string()) }

        offline = true
        client.fetch(request()).use { response ->
            assertEquals(200, response.code)
            assertEquals("v1", response.body!!.string())
            assertNotNull(response.cacheResponse)
            assertNull(response.networkResponse)
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun offline_withNothingCached_failsWithTheNetworkError_notA504() {
        offline = true

        val error = assertThrows(IOException::class.java) { appLikeClient().fetch(request()) }

        assertTrue(error is UnknownHostException)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun offline_aRefreshTheUserAskedFor_isNotAnsweredFromTheCache() {
        server.enqueue(MockResponse().setBody("v1"))
        val client = appLikeClient()
        client.fetch(request()).close()

        offline = true

        assertThrows(IOException::class.java) { client.fetch(request(noCache = true)) }
    }

    @Test
    fun cacheHeaders_areOnlyAddedToSuccessfulResponses() {
        server.enqueue(MockResponse().setBody("ok"))
        server.enqueue(MockResponse().setResponseCode(500))
        val client = appLikeClient()

        client.fetch(request()).use { assertEquals("public, max-age=0", it.header("Cache-Control")) }
        client.fetch(request("/3/movie/2?api_key=abc")).use { assertNull(it.header("Cache-Control")) }
    }

    @Test
    fun unsatisfiedCacheOnlyResponse_isRecognised_butARealGatewayTimeoutIsNot() {
        val client = OkHttpClient.Builder().cache(Cache(cacheDir, 1024L * 1024)).build()
        val cacheOnly = Request.Builder()
            .url(server.url("/3/movie/1?api_key=abc"))
            .cacheControl(CacheControl.FORCE_CACHE)
            .build()
        client.fetch(cacheOnly).use { synthetic ->
            assertEquals(504, synthetic.code)
            assertTrue(isUnsatisfiedCacheOnlyResponse(synthetic))
        }

        server.enqueue(MockResponse().setResponseCode(504))
        client.fetch(request()).use { real ->
            assertEquals(504, real.code)
            assertFalse(isUnsatisfiedCacheOnlyResponse(real))
        }
    }
}
