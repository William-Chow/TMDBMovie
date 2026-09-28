package com.movielist

import androidx.lifecycle.SavedStateHandle
import com.movielist.tmdb.network.model.Cast
import com.movielist.tmdb.network.model.Video
import com.movielist.tmdb.ui.MovieViewModel
import com.movielist.tmdb.util.Utils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MovieViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    /** Every request made through the fakes, as "what:id[:languages]". */
    private val requests = mutableListOf<String>()

    private var movieFailure: Throwable? = null
    private var castResult: suspend () -> List<Cast>? = { listOf(castMember("Second", 1), castMember("First", 0)) }
    private var videosResult: suspend () -> List<Video>? = { listOf(trailer("ko-key", "ko"), trailer("en-key", "en")) }

    private fun castMember(name: String, order: Int?) = Cast().apply {
        this.name = name
        this.order = order
    }

    private fun trailer(key: String, language: String) = Video().apply {
        this.key = key
        site = "YouTube"
        type = "Trailer"
        iso_639_1 = language
    }

    /** Opened the way MovieActivity is: the id is the intent extra in SavedStateHandle. */
    private fun viewModel(saved: SavedStateHandle = SavedStateHandle(mapOf(Utils.movieExtra to 42))) =
        MovieViewModel(
            saved,
            loadMovie = { id ->
                requests += "movie:$id"
                movieFailure?.let { throw it }
                movie(id).apply { original_language = "ko" }
            },
            loadCast = { id ->
                requests += "cast:$id"
                castResult()
            },
            loadVideos = { id, languages ->
                requests += "videos:$id:$languages"
                videosResult()
            }
        )

    @Test
    fun loadsTheMovieInTheIntentExtra_withItsCastAndTrailer() = runTest(main.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(42, viewModel.movie?.id)
        assertNull(viewModel.error)
        assertEquals("billing order", listOf("First", "Second"), viewModel.cast.map { it.name })
        assertTrue(
            "videos asked for in the original language too",
            "videos:42:en,null,ko" in requests
        )
        assertEquals("en-key", viewModel.trailerKey)
    }

    @Test
    fun theCast_isCappedAtFifteen() = runTest(main.dispatcher) {
        castResult = { (30 downTo 1).map { castMember("Actor $it", it) } }

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(MovieViewModel.MAX_CAST_SHOWN, viewModel.cast.size)
        assertEquals("Actor 1", viewModel.cast.first().name)
    }

    @Test
    fun withoutAMovieId_theScreenIsUnavailable_andNothingIsRequested() = runTest(main.dispatcher) {
        val viewModel = viewModel(SavedStateHandle())
        viewModel.retry()
        advanceUntilIdle()

        assertTrue(viewModel.isUnavailable)
        assertNull(viewModel.movie)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun aFailedMovie_showsTheError_andRetryLoadsItAgain() = runTest(main.dispatcher) {
        movieFailure = IOException("offline")
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.error is IOException)
        assertNull(viewModel.movie)
        assertTrue("no trailer without a movie", requests.none { it.startsWith("videos") })

        movieFailure = null
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.error)
        assertEquals(42, viewModel.movie?.id)
        assertEquals(2, requests.count { it == "movie:42" })
    }

    @Test
    fun missingCreditsOrTrailer_doNotFailThePage() = runTest(main.dispatcher) {
        castResult = { throw IOException("no credits") }
        videosResult = { throw IOException("no videos") }

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(42, viewModel.movie?.id)
        assertNull(viewModel.error)
        assertTrue(viewModel.cast.isEmpty())
        assertNull(viewModel.trailerKey)
        assertFalse(viewModel.isUnavailable)
    }
}
