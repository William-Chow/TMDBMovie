package com.movielist.tmdb.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope

/** The home carousel's latest releases, kept across rotation. */
class MainViewModel internal constructor(
    newPager: (scope: CoroutineScope) -> MoviePager
) : ViewModel() {

    /** Used by the default ViewModel factory. */
    constructor() : this({ scope -> discoverPager(scope, genreId = null) })

    val pager: MoviePager = newPager(viewModelScope)

    init {
        pager.loadNext()
    }
}
