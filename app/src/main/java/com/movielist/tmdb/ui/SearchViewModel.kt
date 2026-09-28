package com.movielist.tmdb.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The search field and its results. Results survive rotation; the typed
 * query also survives process death and is searched again afterwards.
 */
class SearchViewModel internal constructor(
    private val savedState: SavedStateHandle,
    private val newPager: (scope: CoroutineScope, query: String) -> MoviePager
) : ViewModel() {

    /** Used by the default ViewModel factory. */
    constructor(savedState: SavedStateHandle) : this(
        savedState,
        newPager = { scope, query -> searchPager(scope, query) }
    )

    /** Exactly what is in the search field. */
    var query by mutableStateOf(savedState.get<String>(KEY_QUERY).orEmpty())
        private set

    /** The term the current results belong to; empty while there is nothing to search for. */
    var submittedQuery by mutableStateOf(searchTerm(query))
        private set

    var pager by mutableStateOf(newPager(viewModelScope, submittedQuery))
        private set

    private var debounce: Job? = null

    init {
        if (submittedQuery.isNotEmpty()) pager.loadNext()
    }

    fun onQueryChange(text: String) {
        query = text
        savedState[KEY_QUERY] = text
        debounce?.cancel()
        val term = searchTerm(text)
        if (term.isEmpty()) {
            submit("")
            return
        }
        // Typing restarts the wait, so only a term the user paused on
        // reaches the network.
        debounce = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            submit(term)
        }
    }

    /** A new term replaces the pager outright, cancelling the old term's request. */
    private fun submit(term: String) {
        if (term == submittedQuery) return
        submittedQuery = term
        pager.cancel()
        pager = newPager(viewModelScope, term)
        if (term.isNotEmpty()) pager.loadNext()
    }

    companion object {
        /** Quiet period after the last keystroke before a search is actually sent. */
        const val SEARCH_DEBOUNCE_MS = 350L
        const val MIN_QUERY_LENGTH = 2
        private const val KEY_QUERY = "query"

        /** The trimmed query if it is long enough to search for, otherwise "". */
        fun searchTerm(text: String): String =
            text.trim().takeIf { it.length >= MIN_QUERY_LENGTH }.orEmpty()
    }
}
