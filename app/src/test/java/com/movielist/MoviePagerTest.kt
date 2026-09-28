package com.movielist

import com.movielist.tmdb.ui.MoviePager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MoviePagerTest {

    private val source = FakeMovieSource(totalPages = 3, pageSize = 2)

    private fun TestScope.newPager(from: FakeMovieSource = source) =
        MoviePager(backgroundScope) { page, fresh -> from.fetch(page, fresh) }

    private val MoviePager.ids get() = movies.map { it.id }

    @Test
    fun loadNext_appendsPagesInOrder_andStopsAtTheLastOne() = runTest {
        val pager = newPager()

        repeat(5) { pager.loadNext() }

        assertEquals(listOf(101, 102, 201, 202, 301, 302), pager.ids)
        assertTrue(pager.endReached)
        assertEquals(listOf(1, 2, 3), source.requestedPages)
        assertTrue("plain page loads may use the cache", source.calls.none { it.second })
    }

    @Test
    fun anEmptyPage_endsTheList_whateverTotalPagesSays() = runTest {
        val pager = MoviePager(backgroundScope) { page, _ -> moviesPage(page, 10, emptyList()) }

        pager.loadNext()

        assertTrue(pager.endReached)
        assertTrue(pager.movies.isEmpty())
        assertFalse(pager.isLoading)
    }

    @Test
    fun aMissingTotalPages_isTreatedAsTheLastPage() = runTest {
        val pager = MoviePager(backgroundScope) { page, _ -> moviesPage(page, null, listOf(movie(1))) }

        pager.loadNext()

        assertTrue(pager.endReached)
    }

    @Test
    fun paging_stopsAtTmdbsLimitOf500Pages() = runTest {
        val huge = FakeMovieSource(totalPages = 1000, pageSize = 1)
        val pager = newPager(huge)

        repeat(600) { pager.loadNext() }

        assertEquals(500, huge.calls.size)
        assertTrue(pager.endReached)
    }

    @Test
    fun loadNext_whileAPageIsInFlight_doesNotFetchItAgain() = runTest {
        val gate = CompletableDeferred<Unit>().also { source.gate = it }
        val pager = newPager()

        pager.loadNext()
        pager.loadNext()
        pager.loadNext()

        assertTrue(pager.isLoading)
        assertTrue(pager.isLoadingFirstPage)
        assertEquals(listOf(1), source.requestedPages)

        gate.complete(Unit)
        runCurrent()

        assertFalse(pager.isLoading)
        assertEquals(listOf(101, 102), pager.ids)
    }

    @Test
    fun aFailedPage_blocksLoading_untilRetried() = runTest {
        val pager = newPager()
        pager.loadNext()
        source.failure = IOException("offline")

        pager.loadNext()
        assertTrue(pager.error is IOException)
        assertFalse(pager.isLoading)

        pager.loadNext()
        assertEquals("no request while the failure is pending", listOf(1, 2), source.requestedPages)

        source.failure = null
        pager.retry()

        assertNull(pager.error)
        assertEquals("the failed page is asked for again", listOf(1, 2, 2), source.requestedPages)
        assertEquals(listOf(101, 102, 201, 202), pager.ids)
    }

    @Test
    fun dismissError_letsTheNextLoadTryAgain() = runTest {
        val pager = newPager()
        source.failure = IOException("offline")
        pager.loadNext()

        source.failure = null
        pager.dismissError()
        pager.loadNext()

        assertNull(pager.error)
        assertEquals(listOf(1, 1), source.requestedPages)
        assertEquals(listOf(101, 102), pager.ids)
    }

    @Test
    fun refresh_replacesTheList_asksForFreshData_andRestartsPaging() = runTest {
        val pager = newPager()
        pager.loadNext()
        pager.loadNext()

        pager.refresh()

        assertEquals(1 to true, source.calls.last())
        assertEquals(listOf(101, 102), pager.ids)
        assertEquals(1, pager.refreshCount)
        assertFalse(pager.isRefreshing)

        pager.loadNext()
        assertEquals(2 to false, source.calls.last())
        assertEquals(listOf(101, 102, 201, 202), pager.ids)
    }

    @Test
    fun refresh_keepsTheOldListUntilTheNewOneArrives_andHoldsOffPageLoads() = runTest {
        val pager = newPager()
        pager.loadNext()
        val gate = CompletableDeferred<Unit>().also { source.gate = it }

        pager.refresh()
        pager.loadNext()

        assertTrue(pager.isRefreshing)
        assertEquals(listOf(101, 102), pager.ids)
        assertEquals(listOf(1, 1), source.requestedPages)

        gate.complete(Unit)
        runCurrent()

        assertFalse(pager.isRefreshing)
        assertEquals(1, pager.refreshCount)
    }

    @Test
    fun refresh_isIgnoredWhileAPageIsLoading() = runTest {
        source.gate = CompletableDeferred()
        val pager = newPager()
        pager.loadNext()

        pager.refresh()

        assertFalse(pager.isRefreshing)
        assertEquals(listOf(1 to false), source.calls)
    }

    @Test
    fun aFailedRefresh_keepsTheListItHad() = runTest {
        val pager = newPager()
        pager.loadNext()
        source.failure = IOException("offline")

        pager.refresh()

        assertEquals(listOf(101, 102), pager.ids)
        assertTrue(pager.error is IOException)
        assertEquals(0, pager.refreshCount)
        assertFalse(pager.isRefreshing)
    }

    @Test
    fun cancel_stopsTheRequestInFlight_withoutReportingAFailure() = runTest {
        val gate = CompletableDeferred<Unit>().also { source.gate = it }
        val pager = newPager()
        pager.loadNext()

        pager.cancel()
        runCurrent()

        assertFalse(pager.isLoading)
        assertNull(pager.error)
        gate.complete(Unit)
        runCurrent()
        assertTrue(pager.movies.isEmpty())
    }
}
