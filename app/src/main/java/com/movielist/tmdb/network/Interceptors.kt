package com.movielist.tmdb.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

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
