package com.movielist.tmdb.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movielist.tmdb.network.RetrofitClient
import com.movielist.tmdb.network.model.Genre
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The Gallery grid and its genre filter. The loaded pages survive rotation;
 * the chosen genre also survives process death.
 */
class GalleryViewModel internal constructor(
    private val savedState: SavedStateHandle,
    private val newPager: (scope: CoroutineScope, genreId: Int?) -> MoviePager,
    private val loadGenres: suspend () -> List<Genre>
) : ViewModel() {

    /** Used by the default ViewModel factory. */
    constructor(savedState: SavedStateHandle) : this(
        savedState,
        newPager = { scope, genreId -> discoverPager(scope, genreId) },
        loadGenres = { RetrofitClient.movieApi.getGenre(RetrofitClient.API_KEY).genres.orEmpty() }
    )

    var genres by mutableStateOf<List<Genre>>(emptyList())
        private set

    /** null means "All": no with_genres filter is sent at all. */
    var selectedGenre by mutableStateOf(restoreSelectedGenre())
        private set

    var pager by mutableStateOf(newPager(viewModelScope, selectedGenre?.id))
        private set

    private var genresJob: Job? = null

    init {
        pager.loadNext()
        fetchGenres()
    }

    /** A new genre is a new list: the old pager's requests are cancelled. */
    fun selectGenre(genre: Genre?) {
        if (genre?.id == selectedGenre?.id) return
        selectedGenre = genre
        savedState[KEY_GENRE_ID] = genre?.id
        savedState[KEY_GENRE_NAME] = genre?.name
        pager.cancel()
        pager = newPager(viewModelScope, genre?.id).also { it.loadNext() }
    }

    /** The filter is a convenience, so a failed genre list is simply retried when the menu opens. */
    fun onGenreMenuOpened() {
        if (genres.isEmpty()) fetchGenres()
    }

    private fun fetchGenres() {
        if (genresJob?.isActive == true) return
        genresJob = viewModelScope.launch {
            genres = try {
                loadGenres()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // Losing the filter must not cost the grid.
                emptyList()
            }
        }
    }

    private fun restoreSelectedGenre(): Genre? {
        val id = savedState.get<Int>(KEY_GENRE_ID) ?: return null
        return Genre().apply {
            this.id = id
            name = savedState.get<String>(KEY_GENRE_NAME)
        }
    }

    private companion object {
        const val KEY_GENRE_ID = "genre_id"
        const val KEY_GENRE_NAME = "genre_name"
    }
}
