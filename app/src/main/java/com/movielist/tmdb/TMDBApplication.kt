package com.movielist.tmdb

import android.app.Application
import com.movielist.tmdb.data.FavoritesStore
import com.movielist.tmdb.network.RetrofitClient

class TMDBApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Both need an application Context and are read from every screen, so
        // they are wired up once here rather than in each activity.
        RetrofitClient.init(this)
        FavoritesStore.init(this)
    }
}
