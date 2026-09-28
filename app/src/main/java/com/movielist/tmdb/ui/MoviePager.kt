package com.movielist.tmdb.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.network.model.Movies
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Accumulating pager over one of TMDB's paged list endpoints.
 *
 * Screens call [loadNext] when the user nears the end of what has already
 * been loaded; the pager keeps track of where it is and refuses overlapping
 * or past-the-end requests, so callers can fire at it freely.
 *
 * Requests run in [parentScope], normally a ViewModel's, so a rotation does
 * not cancel them; [cancel] stops them early, which is what replacing a pager
 * with a new one (another genre, another search term) does.
 *
 * [fetch] gets `fresh = true` only for a [refresh] the user asked for, which
 * should bypass the HTTP cache; see RetrofitClient.cacheControl.
 */
class MoviePager(
    parentScope: CoroutineScope,
    private val fetch: suspend (page: Int, fresh: Boolean) -> Movies
) {
    // A child of the owner's job: cancelled along with it, or alone by cancel().
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)

    var movies by mutableStateOf<List<Movie>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set

    /** The last failure, kept until [retry], [dismissError] or a successful [refresh]. */
    var error by mutableStateOf<Throwable?>(null)
        private set
    var endReached by mutableStateOf(false)
        private set
    var isRefreshing by mutableStateOf(false)
        private set

    /**
     * Counts refreshes that replaced the list. Screens key their scroll state
     * on it, so a refreshed list starts from the top again.
     */
    var refreshCount by mutableIntStateOf(0)
        private set

    private var nextPage = 1

    /** True while the very first page is in flight and there is nothing to show yet. */
    val isLoadingFirstPage: Boolean get() = isLoading && movies.isEmpty()

    fun loadNext() {
        if (isLoading || isRefreshing || endReached || error != null) return
        load()
    }

    /** Clears the last failure and re-attempts the page that failed. */
    fun retry() {
        if (isLoading || isRefreshing) return
        error = null
        load()
    }

    /**
     * Throws the loaded pages away and reloads from the first one — what a
     * pull-to-refresh gesture means. The existing list stays on screen until
     * the new first page arrives, so the grid does not blink empty.
     */
    fun refresh() {
        // A page load already in flight would append its result on top of the
        // refreshed list, interleaving two different page sequences.
        if (isRefreshing || isLoading) return
        launchNow {
            isRefreshing = true
            try {
                val response = fetch(1, true)
                val page = response.results.orEmpty()
                val lastPage = minOf(response.total_pages ?: 1, MAX_PAGE)
                movies = page
                endReached = page.isEmpty() || lastPage <= 1
                nextPage = 2
                error = null
                refreshCount++
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                error = throwable
            } finally {
                isRefreshing = false
            }
        }
    }

    /**
     * Drops a failure that has already been reported some other way, so that a
     * later [loadNext] is allowed to try the same page again.
     */
    fun dismissError() {
        error = null
    }

    /** Stops whatever this pager has in flight; for when a screen replaces it. */
    fun cancel() {
        job.cancel()
    }

    private fun load() {
        launchNow {
            isLoading = true
            try {
                val response = fetch(nextPage, false)
                val page = response.results.orEmpty()
                // TMDB caps paging at 500 pages regardless of what total_pages says.
                val lastPage = minOf(response.total_pages ?: nextPage, MAX_PAGE)
                movies = movies + page
                endReached = page.isEmpty() || nextPage >= lastPage
                nextPage++
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                error = throwable
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Runs [block] right away, up to its first suspension, so the flag it sets
     * is already up when this returns and an immediate second call is refused.
     */
    private fun launchNow(block: suspend CoroutineScope.() -> Unit) {
        scope.launch(start = CoroutineStart.UNDISPATCHED, block = block)
    }

    private companion object {
        const val MAX_PAGE = 500
    }
}
