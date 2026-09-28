package com.movielist

import com.movielist.tmdb.network.MovieApi
import com.movielist.tmdb.network.RetrofitClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

class MovieApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: MovieApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/3/"))
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(MovieApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueuePage(body: String = EMPTY_PAGE) {
        server.enqueue(MockResponse().setBody(body).setHeader("Content-Type", "application/json"))
    }

    @Test
    fun discover_sendsNoCacheHeaderOnlyForARefresh(): Unit = runBlocking {
        enqueuePage()
        enqueuePage()

        api.getDiscover("key", 2, null, "2025-01-31", RetrofitClient.cacheControl(fresh = false))
        api.getDiscover("key", 1, 28, "2025-01-31", RetrofitClient.cacheControl(fresh = true))

        val normal = server.takeRequest()
        assertEquals("/3/discover/movie", normal.requestUrl!!.encodedPath)
        assertEquals("key", normal.requestUrl!!.queryParameter("api_key"))
        assertEquals("2", normal.requestUrl!!.queryParameter("page"))
        assertNull(normal.requestUrl!!.queryParameter("with_genres"))
        assertNull(normal.getHeader("Cache-Control"))

        val refresh = server.takeRequest()
        assertEquals("28", refresh.requestUrl!!.queryParameter("with_genres"))
        assertEquals("no-cache", refresh.getHeader("Cache-Control"))
    }

    @Test
    fun discover_onlyAsksForMoviesAlreadyReleased(): Unit = runBlocking {
        enqueuePage()

        api.getDiscover("key", 1, null, "2025-01-31", null)

        val url = server.takeRequest().requestUrl!!
        assertEquals("release_date.desc", url.queryParameter("sort_by"))
        assertEquals("2025-01-31", url.queryParameter("release_date.lte"))
        assertEquals("10", url.queryParameter("vote_count.gte"))
    }

    @Test
    fun search_sendsTheTermAndTheRefreshHeader(): Unit = runBlocking {
        enqueuePage()

        api.getSearch("key", "the matrix", 1, RetrofitClient.cacheControl(fresh = true))

        val request = server.takeRequest()
        assertEquals("/3/search/movie", request.requestUrl!!.encodedPath)
        assertEquals("the matrix", request.requestUrl!!.queryParameter("query"))
        assertEquals("no-cache", request.getHeader("Cache-Control"))
    }

    @Test
    fun moviesPage_isParsed_andUnknownFieldsAreIgnored(): Unit = runBlocking {
        enqueuePage(
            """
            {"page": 1, "total_pages": 3, "total_results": 41, "unexpected": true,
             "results": [{"id": 5, "title": "Four Rooms", "poster_path": "/p.jpg",
                          "release_date": "1995-12-09", "vote_average": 5.9, "brand_new_field": 1}]}
            """.trimIndent()
        )

        val page = api.getDiscover("key", 1, null, "2025-01-31", null)

        assertEquals(3, page.total_pages)
        val movie = page.results!!.single()
        assertEquals(5, movie.id)
        assertEquals("Four Rooms", movie.title)
        assertEquals("/p.jpg", movie.poster_path)
    }

    private companion object {
        const val EMPTY_PAGE = """{"page": 1, "results": [], "total_pages": 1, "total_results": 0}"""
    }
}
