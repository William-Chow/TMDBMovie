package com.movielist.tmdb.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.network.model.Movies
import com.movielist.tmdb.util.Utils
import kotlinx.coroutines.CancellationException

/**
 * Accumulating pager over one of TMDB's paged list endpoints.
 *
 * Screens own a pager and call [loadNext] when the user nears the end of what
 * has already been loaded; the pager keeps track of where it is and refuses
 * overlapping or past-the-end requests, so callers can fire at it freely.
 */
class MoviePager(private val fetch: suspend (page: Int) -> Movies) {

    var movies by mutableStateOf<List<Movie>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var endReached by mutableStateOf(false)
        private set
    var isRefreshing by mutableStateOf(false)
        private set

    private var nextPage = 1

    /** True while the very first page is in flight and there is nothing to show yet. */
    val isLoadingFirstPage: Boolean get() = isLoading && movies.isEmpty()

    suspend fun loadNext(context: Context) {
        if (isLoading || isRefreshing || endReached || error != null) return
        load(context)
    }

    /** Clears the last failure and re-attempts the page that failed. */
    suspend fun retry(context: Context) {
        if (isLoading || isRefreshing) return
        error = null
        load(context)
    }

    /**
     * Throws the loaded pages away and reloads from the first one — what a
     * pull-to-refresh gesture means. The existing list stays on screen until
     * the new first page arrives, so the grid does not blink empty.
     */
    suspend fun refresh(context: Context) {
        // A page load already in flight would append its result on top of the
        // refreshed list, interleaving two different page sequences.
        if (isRefreshing || isLoading) return
        isRefreshing = true
        try {
            val response = fetch(1)
            val page = response.results.orEmpty()
            val lastPage = minOf(response.total_pages ?: 1, MAX_PAGE)
            movies = page
            endReached = page.isEmpty() || lastPage <= 1
            nextPage = 2
            error = null
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            error = Utils.errorMessage(context, throwable)
        } finally {
            isRefreshing = false
        }
    }

    /**
     * Drops a failure that has already been reported some other way, so that a
     * later [loadNext] is allowed to try the same page again.
     */
    fun dismissError() {
        error = null
    }

    private suspend fun load(context: Context) {
        isLoading = true
        try {
            val response = fetch(nextPage)
            val page = response.results.orEmpty()
            // TMDB caps paging at 500 pages regardless of what total_pages says.
            val lastPage = minOf(response.total_pages ?: nextPage, MAX_PAGE)
            movies = movies + page
            endReached = page.isEmpty() || nextPage >= lastPage
            nextPage++
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            error = Utils.errorMessage(context, throwable)
        } finally {
            isLoading = false
        }
    }

    private companion object {
        const val MAX_PAGE = 500
    }
}

/**
 * Remembers a pager, rebuilding it whenever [key] changes — a new search term
 * or a new genre filter starts its own list from page 1.
 */
@Composable
fun rememberMoviePager(key: Any?, fetch: suspend (page: Int) -> Movies): MoviePager =
    remember(key) { MoviePager(fetch) }
