package com.movielist.tmdb.network

import android.content.Context
import androidx.annotation.Keep
import com.movielist.tmdb.BuildConfig
import com.movielist.tmdb.util.Utils
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

@Keep
object RetrofitClient {

    // Base URL
    private const val BASE_URL = "https://api.themoviedb.org/3/"

    /** Supplied through local.properties; see app/build.gradle. */
    const val API_KEY: String = BuildConfig.TMDB_API_KEY

    private const val CACHE_SIZE_BYTES = 10L * 1024 * 1024
    private const val ONLINE_MAX_AGE_SECONDS = 60
    private const val OFFLINE_MAX_STALE_DAYS = 7

    private lateinit var appContext: Context

    /** Called once from the Application, before any screen makes a request. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * TMDB does not send cache headers of its own, so responses are given a
     * short freshness window on the way in. Requests made with no network are
     * then answered from that cache instead of failing outright.
     */
    private val cacheHeaderInterceptor = Interceptor { chain ->
        chain.proceed(chain.request())
            .newBuilder()
            .header("Cache-Control", "public, max-age=$ONLINE_MAX_AGE_SECONDS")
            .removeHeader("Pragma")
            .build()
    }

    private val offlineCacheInterceptor = Interceptor { chain ->
        val request = if (Utils.checkInternetConnection(appContext)) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .cacheControl(
                    CacheControl.Builder()
                        .onlyIfCached()
                        .maxStale(OFFLINE_MAX_STALE_DAYS, TimeUnit.DAYS)
                        .build()
                )
                .build()
        }
        chain.proceed(request)
    }

    // One Retrofit instance for the whole app; every screen shares it so the
    // underlying OkHttp connection pool, cache and thread pool are reused.
    val movieApi: MovieApi by lazy {
        val client = OkHttpClient.Builder()
            .cache(Cache(File(appContext.cacheDir, "http"), CACHE_SIZE_BYTES))
            .addInterceptor(offlineCacheInterceptor)
            .addNetworkInterceptor(cacheHeaderInterceptor)
            .build()

        Retrofit.Builder()
            .client(client)
            .baseUrl(BASE_URL)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(MovieApi::class.java)
    }
}
