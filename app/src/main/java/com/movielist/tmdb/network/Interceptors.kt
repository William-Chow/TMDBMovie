package com.movielist.tmdb.network

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

/**
 * Raised instead of sending a request that has no API key. TMDB would only
 * answer 401, and "the server returned an error (401)" hides the real
 * problem: the app was built without a key (see README, "Setup").
 *
 * It is an [IOException] because that is what OkHttp expects interceptors to
 * throw; anything else escapes onto OkHttp's dispatcher thread.
 */
class MissingApiKeyException : IOException("No TMDB API key configured")

/** Fails a request that carries no `api_key` before it reaches the cache or the network. */
class ApiKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.queryParameter("api_key").isNullOrBlank()) {
            throw MissingApiKeyException()
        }
        return chain.proceed(request)
    }
}

/**
 * Network interceptor. TMDB sends no cache headers of its own, so successful
 * responses are given a freshness window of [maxAgeSeconds] on the way in;
 * that is also what lets [StaleCacheFallbackInterceptor] find them later.
 */
class CacheHeaderInterceptor(private val maxAgeSeconds: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (!response.isSuccessful) return response
        return response.newBuilder()
            .header("Cache-Control", "public, max-age=$maxAgeSeconds")
            .removeHeader("Pragma")
            .build()
    }
}

/**
 * Application interceptor: when the network fails, answers from the cache
 * instead, however stale (up to [maxStaleDays]).
 *
 * The network always gets the first try. Deciding "offline" up front from
 * the connectivity state was wrong both ways: a network that is not (yet)
 * validated can still work, and a request forced to the cache that found
 * nothing there came back as OkHttp's synthetic HTTP 504, which the app
 * reported as a server error.
 *
 * A request marked no-cache (a refresh the user asked for) is not answered
 * from the cache: the user wants fresh data and should hear that there is
 * none. When nothing usable is cached, the original [IOException] is
 * rethrown, and the error screens word it as no connection.
 */
class StaleCacheFallbackInterceptor(private val maxStaleDays: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        try {
            return chain.proceed(request)
        } catch (networkFailure: IOException) {
            if (networkFailure is MissingApiKeyException ||
                request.cacheControl.noCache ||
                chain.call().isCanceled()
            ) {
                throw networkFailure
            }
            val cached = chain.proceed(
                request.newBuilder()
                    .cacheControl(
                        CacheControl.Builder()
                            .onlyIfCached()
                            .maxStale(maxStaleDays, TimeUnit.DAYS)
                            .build()
                    )
                    .build()
            )
            if (isUnsatisfiedCacheOnlyResponse(cached)) {
                cached.close()
                throw networkFailure
            }
            return cached
        }
    }
}

/**
 * True for the 504 that OkHttp makes up itself when a request may only be
 * answered from the cache and the cache has nothing: no server was involved,
 * so it means "offline", not "the server failed".
 */
fun isUnsatisfiedCacheOnlyResponse(response: Response): Boolean =
    response.code == HttpURLConnection.HTTP_GATEWAY_TIMEOUT &&
        response.networkResponse == null &&
        response.cacheResponse == null
