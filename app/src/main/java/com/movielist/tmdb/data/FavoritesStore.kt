package com.movielist.tmdb.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.movielist.tmdb.network.model.Movie

/**
 * The user's saved movies, kept in SharedPreferences as JSON.
 *
 * [favorites] is Compose state, so every screen reading it updates the moment
 * a movie is saved or removed anywhere in the app.
 */
object FavoritesStore {

    private const val PREFS_NAME = "favorites"
    private const val KEY_MOVIES = "movies"

    private val mapper = ObjectMapper()
    private lateinit var prefs: SharedPreferences

    var favorites by mutableStateOf<List<FavoriteMovie>>(emptyList())
        private set

    /** Called once from the Application, before any screen reads the list. */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        favorites = read()
    }

    fun isFavorite(movieId: Int?): Boolean =
        movieId != null && favorites.any { it.id == movieId }

    /** Saves the movie, or removes it when it is already saved. */
    fun toggle(movie: Movie) {
        val id = movie.id ?: return
        val updated = if (favorites.any { it.id == id }) {
            favorites.filterNot { it.id == id }
        } else {
            favorites + FavoriteMovie().apply {
                this.id = id
                this.title = movie.title
                this.poster_path = movie.poster_path
            }
        }
        favorites = updated
        write(updated)
    }

    private fun read(): List<FavoriteMovie> {
        val json = prefs.getString(KEY_MOVIES, null) ?: return emptyList()
        return try {
            mapper.readValue(json, object : TypeReference<List<FavoriteMovie>>() {})
        } catch (_: Exception) {
            // Unreadable or from an older shape; start over rather than crash.
            emptyList()
        }
    }

    private fun write(movies: List<FavoriteMovie>) {
        prefs.edit().putString(KEY_MOVIES, mapper.writeValueAsString(movies)).apply()
    }
}
