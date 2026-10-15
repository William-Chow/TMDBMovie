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
import kotlinx.coroutines.ensureActive
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

    /**
     * The last failed page load, kept until [retry], [dismissError] or a
     * successful [refresh]. No further page is loaded while it is set.
     */
    var error by mutableStateOf<Throwable?>(null)
        private set

    /**
     * A [refresh] that failed while the list it was to replace is still on
     * screen. That list stays usable and paging carries on, so screens only
     * report this, wherever the user has scrolled to, then [dismissRefreshError].
     */
    var refreshError by mutableStateOf<Throwable?>(null)
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
    private var loadJob: Job? = null

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
        // Before the first page arrives there is nothing to refresh, and that
        // page is already on its way.
        if (isRefreshing || isLoadingFirstPage) return
        // A next page still in flight would be appended to the refreshed list,
        // interleaving two page sequences, so the refresh replaces it.
        loadJob?.cancel()
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
                refreshError = null
                refreshCount++
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                if (movies.isEmpty()) {
                    // Nothing on screen to keep: this is the screen's error,
                    // and its retry asks for the first page again.
                    nextPage = 1
                    error = throwable
                } else {
                    refreshError = throwable
                }
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

    /** Drops a failed refresh once it has been reported. */
    fun dismissRefreshError() {
        refreshError = null
    }

    /** Stops whatever this pager has in flight; for when a screen replaces it. */
    fun cancel() {
        job.cancel()
    }

    private fun load() {
        loadJob = launchNow {
            isLoading = true
            try {
                val response = fetch(nextPage, false)
                // Replaced by a refresh while the page was on its way.
                ensureActive()
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
    private fun launchNow(block: suspend CoroutineScope.() -> Unit): Job =
        scope.launch(start = CoroutineStart.UNDISPATCHED, block = block)

    private companion object {
        const val MAX_PAGE = 500
    }
}
