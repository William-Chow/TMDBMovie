package com.movielist.tmdb.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movielist.tmdb.network.RetrofitClient
import com.movielist.tmdb.network.model.Cast
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.network.model.Video
import com.movielist.tmdb.util.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * One movie's detail screen: the movie, its cast and its trailer, kept across
 * rotation. The movie id is the activity's intent extra, which the default
 * factory hands to [SavedStateHandle], so after process death the same movie
 * is loaded again.
 */
class MovieViewModel internal constructor(
    savedState: SavedStateHandle,
    private val loadMovie: suspend (id: Int) -> Movie,
    private val loadCast: suspend (id: Int) -> List<Cast>?,
    private val loadVideos: suspend (id: Int, languages: String) -> List<Video>?
) : ViewModel() {

    /** Used by the default ViewModel factory. */
    constructor(savedState: SavedStateHandle) : this(
        savedState,
        loadMovie = { id -> RetrofitClient.movieApi.getMovie(id, RetrofitClient.API_KEY) },
        loadCast = { id -> RetrofitClient.movieApi.getCredits(id, RetrofitClient.API_KEY).cast },
        loadVideos = { id, languages ->
            RetrofitClient.movieApi.getVideo(id, RetrofitClient.API_KEY, languages).results
        }
    )

    val movieId: Int = savedState.get<Int>(Utils.movieExtra) ?: 0

    /** Opened without a movie id: there is nothing to load. */
    val isUnavailable: Boolean get() = movieId == 0

    var movie by mutableStateOf<Movie?>(null)
        private set

    /** Why the movie itself failed to load; cleared by [retry]. */
    var error by mutableStateOf<Throwable?>(null)
        private set

    var cast by mutableStateOf<List<Cast>>(emptyList())
        private set

    var trailerKey by mutableStateOf<String?>(null)
        private set

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        if (isUnavailable) return
        loadJob?.cancel()
        movie = null
        error = null
        trailerKey = null
        loadJob = viewModelScope.launch {
            // Neither missing credits nor a missing trailer is a reason to
            // fail the page, so they are fetched on the side.
            launch {
                cast = orNull { loadCast(movieId) }.orEmpty()
                    .sortedBy { it.order ?: Int.MAX_VALUE }
                    .take(MAX_CAST_SHOWN)
            }
            val loaded = try {
                loadMovie(movieId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                error = throwable
                return@launch
            }
            movie = loaded
            // Waits for the movie: many non-English films only have a trailer
            // filed under their original language, which is asked for too.
            trailerKey = Utils.pickTrailerKey(
                orNull { loadVideos(movieId, Utils.videoLanguages(loaded.original_language)) }
            )
        }
    }

    private suspend fun <T> orNull(block: suspend () -> T): T? = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        null
    }

    internal companion object {
        const val MAX_CAST_SHOWN = 15
    }
}
