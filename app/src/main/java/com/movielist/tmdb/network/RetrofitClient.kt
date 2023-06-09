package com.movielist.tmdb.network

import androidx.annotation.Keep
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.*

@Keep
object RetrofitClient {

    // Base URL
    private const val BASE_URL = "https://api.themoviedb.org/3/"
    const val API_KEY = "1ee04cdd24bdc8497ec43f739fd3b5a5"

    private val okHttpClient = OkHttpClient()
        .newBuilder()
        .addInterceptor(AuthorizationInterceptor)
        .addInterceptor(RequestInterceptor)
        .build()

    fun getClient(): Retrofit =
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(BASE_URL)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
}

object RequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return chain.proceed(request)
    }
}

object AuthorizationInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestWithHeader = chain.request()
            .newBuilder()
            .header(
                "Authorization", UUID.randomUUID().toString()
            ).build()
        return chain.proceed(requestWithHeader)
    }
}