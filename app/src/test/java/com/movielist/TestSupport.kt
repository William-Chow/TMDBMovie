package com.movielist

import com.movielist.tmdb.network.model.Genre
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.network.model.Movies
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Routes Dispatchers.Main, and with it viewModelScope, to a test dispatcher. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

fun movie(id: Int): Movie = Movie().apply {
    this.id = id
    title = "Movie $id"
}

fun genre(id: Int, name: String): Genre = Genre().apply {
    this.id = id
    this.name = name
}

fun moviesPage(page: Int, totalPages: Int?, results: List<Movie>): Movies = Movies().apply {
    this.page = page
    total_pages = totalPages
    this.results = results
}

/**
 * A scripted paged endpoint: [totalPages] pages of [pageSize] movies whose
 * ids are page * 100 + n. Records every call as (page, fresh); a [gate]
 * holds calls until it completes, and a [failure] is thrown instead of
 * answering while it is set.
 */
class FakeMovieSource(private val totalPages: Int = 3, private val pageSize: Int = 2) {
    val calls = mutableListOf<Pair<Int, Boolean>>()
    var gate: CompletableDeferred<Unit>? = null
    var failure: Throwable? = null

    val requestedPages: List<Int> get() = calls.map { it.first }

    suspend fun fetch(page: Int, fresh: Boolean): Movies {
        calls += page to fresh
        gate?.await()
        failure?.let { throw it }
        return moviesPage(page, totalPages, (1..pageSize).map { movie(page * 100 + it) })
    }
}
