package com.movielist.tmdb.ui

import com.movielist.tmdb.network.ReleaseDateCutoff
import com.movielist.tmdb.network.RetrofitClient
import com.movielist.tmdb.util.Utils
import kotlinx.coroutines.CoroutineScope

/**
 * The latest releases, newest first. [genreId] narrows them on the server,
 * so a genre with few recent releases still fills the grid.
 */
fun discoverPager(scope: CoroutineScope, genreId: Int?): MoviePager {
    val cutoff = ListCutoff()
    return MoviePager(scope) { page, fresh ->
        RetrofitClient.movieApi.getDiscover(
            RetrofitClient.API_KEY,
            page,
            genreId,
            ReleaseDateCutoff(cutoff.forPage(page)),
            RetrofitClient.cacheControl(fresh)
        )
    }
}

/**
 * The release date cutoff of one list. It is taken afresh whenever page 1 is
 * fetched (the first load, a refresh) and kept for the pages after it: a list
 * scrolled past midnight would otherwise be paged against two cutoffs, and
 * the films released in between would shift its pages, repeating some
 * movies and skipping others.
 */
internal class ListCutoff(private val today: () -> String = { Utils.today() }) {
    private var date = today()

    fun forPage(page: Int): String {
        if (page == 1) date = today()
        return date
    }
}

/** Title search results for [query]. */
fun searchPager(scope: CoroutineScope, query: String): MoviePager =
    MoviePager(scope) { page, fresh ->
        RetrofitClient.movieApi.getSearch(
            RetrofitClient.API_KEY,
            query,
            page,
            RetrofitClient.cacheControl(fresh)
        )
    }
