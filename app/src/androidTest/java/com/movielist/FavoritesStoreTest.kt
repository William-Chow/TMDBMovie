package com.movielist

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.movielist.tmdb.data.FavoritesStore
import com.movielist.tmdb.network.model.Movie
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Favourites are the only thing the app keeps on the device. This checks
 * their round trip through SharedPreferences and Jackson on a real device,
 * using a movie id no real favourite will have, and leaves it removed.
 */
@RunWith(AndroidJUnit4::class)
class FavoritesStoreTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val movie = Movie().apply {
        id = TEST_MOVIE_ID
        title = "Instrumented test movie"
        poster_path = "/instrumented-test.jpg"
    }

    @Before
    fun setUp() {
        FavoritesStore.init(context)
        removeTestMovie()
    }

    @After
    fun tearDown() {
        removeTestMovie()
    }

    private fun removeTestMovie() {
        if (FavoritesStore.isFavorite(TEST_MOVIE_ID)) FavoritesStore.toggle(movie)
    }

    @Test
    fun aSavedMovie_readsBackWithItsTitleAndPoster() {
        FavoritesStore.toggle(movie)
        assertTrue(FavoritesStore.isFavorite(TEST_MOVIE_ID))

        // What a fresh start of the app does.
        FavoritesStore.init(context)

        val saved = FavoritesStore.favorites.single { it.id == TEST_MOVIE_ID }
        assertEquals("Instrumented test movie", saved.title)
        assertEquals("/instrumented-test.jpg", saved.poster_path)
    }

    @Test
    fun togglingTwice_removesTheMovieAgain() {
        FavoritesStore.toggle(movie)
        FavoritesStore.toggle(movie)

        FavoritesStore.init(context)

        assertFalse(FavoritesStore.isFavorite(TEST_MOVIE_ID))
    }

    private companion object {
        /** Far beyond any real TMDB movie id. */
        const val TEST_MOVIE_ID = Int.MAX_VALUE - 7
    }
}
