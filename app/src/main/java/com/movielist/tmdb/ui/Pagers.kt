package com.movielist.tmdb.ui

import com.movielist.tmdb.network.RetrofitClient
import com.movielist.tmdb.util.Utils
import kotlinx.coroutines.CoroutineScope

/**
 * The latest releases, newest first. [genreId] narrows them on the server,
 * so a genre with few recent releases still fills the grid.
 */
fun discoverPager(scope: CoroutineScope, genreId: Int?): MoviePager =
    MoviePager(scope) { page, fresh ->
        RetrofitClient.movieApi.getDiscover(
            RetrofitClient.API_KEY,
            page,
            genreId,
            Utils.today(),
            RetrofitClient.cacheControl(fresh)
        )
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
