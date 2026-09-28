package com.movielist

import androidx.lifecycle.SavedStateHandle
import com.movielist.tmdb.network.model.Genre
import com.movielist.tmdb.ui.GalleryViewModel
import com.movielist.tmdb.ui.MainViewModel
import com.movielist.tmdb.ui.MoviePager
import com.movielist.tmdb.ui.SearchViewModel
import com.movielist.tmdb.ui.SearchViewModel.Companion.SEARCH_DEBOUNCE_MS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    @get:Rule
    val main = MainDispatcherRule()

    /** Every page request made through the fake pagers, as "key#page". */
    private val requests = mutableListOf<String>()

    private fun fakePager(scope: kotlinx.coroutines.CoroutineScope, key: Any?) =
        MoviePager(scope) { page, _ ->
            requests += "$key#$page"
            moviesPage(page, 1, listOf(movie(page)))
        }

    // --- Home -------------------------------------------------------------

    @Test
    fun home_startsLoadingTheLatestReleasesRightAway() = runTest(main.dispatcher) {
        val source = FakeMovieSource()

        val viewModel = MainViewModel { scope -> MoviePager(scope) { page, fresh -> source.fetch(page, fresh) } }

        assertEquals(listOf(1 to false), source.calls)
        assertEquals(2, viewModel.pager.movies.size)
    }

    // --- Search -----------------------------------------------------------

    private fun searchViewModel(saved: SavedStateHandle = SavedStateHandle()) =
        SearchViewModel(saved) { scope, query -> fakePager(scope, query) }

    @Test
    fun search_onlySendsTheTermTheUserPausedOn() = runTest(main.dispatcher) {
        val viewModel = searchViewModel()

        viewModel.onQueryChange("m")
        viewModel.onQueryChange("ma")
        advanceTimeBy(SEARCH_DEBOUNCE_MS - 1)
        viewModel.onQueryChange("mat")
        advanceTimeBy(SEARCH_DEBOUNCE_MS - 1)

        assertTrue(requests.isEmpty())
        assertEquals("", viewModel.submittedQuery)

        advanceTimeBy(2)

        assertEquals(listOf("mat#1"), requests)
        assertEquals("mat", viewModel.submittedQuery)
        assertEquals("mat", viewModel.query)
    }

    @Test
    fun search_aQueryTooShortToSearch_clearsTheResultsAtOnce() = runTest(main.dispatcher) {
        val viewModel = searchViewModel()
        viewModel.onQueryChange("matrix")
        advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
        assertEquals("matrix", viewModel.submittedQuery)

        viewModel.onQueryChange(" m ")

        assertEquals("", viewModel.submittedQuery)
    }

    @Test
    fun search_theSameTermAgain_keepsTheCurrentResults() = runTest(main.dispatcher) {
        val viewModel = searchViewModel()
        viewModel.onQueryChange("matrix")
        advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
        val results = viewModel.pager

        viewModel.onQueryChange("matrix  ")
        advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)

        assertSame(results, viewModel.pager)
        assertEquals(listOf("matrix#1"), requests)
    }

    @Test
    fun search_theQuerySurvivesProcessDeath_andIsSearchedAgain() = runTest(main.dispatcher) {
        val saved = SavedStateHandle()
        searchViewModel(saved).onQueryChange("dune")
        requests.clear()

        val restored = searchViewModel(saved)

        assertEquals("dune", restored.query)
        assertEquals("dune", restored.submittedQuery)
        assertEquals(listOf("dune#1"), requests)
    }

    @Test
    fun search_aNewTerm_cancelsTheRequestStillRunningForTheOldOne() = runTest(main.dispatcher) {
        var slowRequestCancelled = false
        val viewModel = SearchViewModel(SavedStateHandle()) { scope, query ->
            MoviePager(scope) { page, _ ->
                if (query == "slow") {
                    try {
                        awaitCancellation()
                    } finally {
                        slowRequestCancelled = true
                    }
                }
                moviesPage(page, 1, listOf(movie(page)))
            }
        }
        viewModel.onQueryChange("slow")
        advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
        assertTrue(viewModel.pager.isLoading)

        viewModel.onQueryChange("fast")
        advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
        runCurrent()

        assertTrue(slowRequestCancelled)
        assertEquals("fast", viewModel.submittedQuery)
        assertEquals(1, viewModel.pager.movies.size)
    }

    // --- Gallery ----------------------------------------------------------

    private var genreList: () -> List<Genre> = { listOf(genre(28, "Action"), genre(18, "Drama")) }

    private fun galleryViewModel(saved: SavedStateHandle = SavedStateHandle()) = GalleryViewModel(
        saved,
        newPager = { scope, genreId -> fakePager(scope, genreId) },
        loadGenres = { genreList() }
    )

    @Test
    fun gallery_startsWithAllGenres_andLoadsTheGridAndTheGenreList() = runTest(main.dispatcher) {
        val viewModel = galleryViewModel()
        runCurrent()

        assertNull(viewModel.selectedGenre)
        assertEquals(listOf("null#1"), requests)
        assertEquals(listOf("Action", "Drama"), viewModel.genres.map { it.name })
    }

    @Test
    fun gallery_choosingAGenre_startsANewList() = runTest(main.dispatcher) {
        val viewModel = galleryViewModel()
        val allGenres = viewModel.pager

        viewModel.selectGenre(genre(18, "Drama"))
        assertNotSame(allGenres, viewModel.pager)
        assertEquals(listOf("null#1", "18#1"), requests)

        viewModel.selectGenre(genre(18, "Drama"))
        assertEquals("choosing it again changes nothing", listOf("null#1", "18#1"), requests)

        viewModel.selectGenre(null)
        assertNull(viewModel.selectedGenre)
        assertEquals(listOf("null#1", "18#1", "null#1"), requests)
    }

    @Test
    fun gallery_theChosenGenreSurvivesProcessDeath() = runTest(main.dispatcher) {
        val saved = SavedStateHandle()
        galleryViewModel(saved).selectGenre(genre(28, "Action"))
        requests.clear()

        val restored = galleryViewModel(saved)

        assertEquals(28, restored.selectedGenre?.id)
        assertEquals("Action", restored.selectedGenre?.name)
        assertEquals(listOf("28#1"), requests)
    }

    @Test
    fun gallery_aFailedGenreList_isRetriedWhenTheMenuOpens() = runTest(main.dispatcher) {
        genreList = { throw IOException("offline") }
        val viewModel = galleryViewModel()
        runCurrent()
        assertTrue(viewModel.genres.isEmpty())
        assertEquals("the grid still loads", listOf("null#1"), requests)

        genreList = { listOf(genre(35, "Comedy")) }
        viewModel.onGenreMenuOpened()
        runCurrent()

        assertEquals(listOf("Comedy"), viewModel.genres.map { it.name })
    }
}
