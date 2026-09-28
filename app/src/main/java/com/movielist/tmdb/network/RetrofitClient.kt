package com.movielist.tmdb.network

import android.content.Context
import androidx.annotation.Keep
import com.movielist.tmdb.BuildConfig
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File

@Keep
object RetrofitClient {

    // Base URL
    private const val BASE_URL = "https://api.themoviedb.org/3/"

    /**
     * Supplied through local.properties; see app/build.gradle. Empty in a
     * build made without one, which [ApiKeyInterceptor] reports as such.
     */
    const val API_KEY: String = BuildConfig.TMDB_API_KEY

    private const val CACHE_SIZE_BYTES = 10L * 1024 * 1024
    private const val ONLINE_MAX_AGE_SECONDS = 60
    private const val OFFLINE_MAX_STALE_DAYS = 7

    private lateinit var appContext: Context

    /**
     * Cache-Control for a list request. A refresh the user asked for has to
     * reach the network even while the cached copy is still within
     * [ONLINE_MAX_AGE_SECONDS]; any other request sends no header and may be
     * answered from the cache.
     */
    fun cacheControl(fresh: Boolean): String? = if (fresh) "no-cache" else null

    /** Called once from the Application, before any screen makes a request. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // One Retrofit instance for the whole app; every screen shares it so the
    // underlying OkHttp connection pool, cache and thread pool are reused.
    val movieApi: MovieApi by lazy {
        val client = OkHttpClient.Builder()
            .cache(Cache(File(appContext.cacheDir, "http"), CACHE_SIZE_BYTES))
            // First, so a build without a key never even consults the cache.
            .addInterceptor(ApiKeyInterceptor())
            // Network first; a copy up to a week old when that fails.
            .addInterceptor(StaleCacheFallbackInterceptor(OFFLINE_MAX_STALE_DAYS))
            // TMDB sends no cache headers, so responses get a short max-age.
            .addNetworkInterceptor(CacheHeaderInterceptor(ONLINE_MAX_AGE_SECONDS))
            .build()

        Retrofit.Builder()
            .client(client)
            .baseUrl(BASE_URL)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(MovieApi::class.java)
    }
}
