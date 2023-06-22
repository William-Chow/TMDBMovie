package com.movielist.tmdb.network

import androidx.annotation.Keep
import com.movielist.tmdb.network.model.*
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Keep
interface MovieApi {

    // Get Movie List
    @GET("discover/movie?sort_by=release_date.desc&include_adult=true&include_video=true")
    fun getDiscover(@Query("api_key") api_key: String, @Query("page") page: Int): Call<Movies>

    // Get Movie Item
    // movie/76600?api_key={api_key}&language=en-US
    @GET("movie/{movie_id}?language=en-US")
    fun getMovie(@Path("movie_id") movie_id: Int, @Query("api_key") api_key: String): Call<Movie>

    // Get Search
    // search/movie?api_key={api_key}&language=en-US
    @GET("search/movie?language=en-US")
    fun getSearch(
        @Query("api_key") api_key: String,
        @Query("query") query: String,
        @Query("page") page: Int
    ): Call<Movies>

    // Ge Genre
    // genre/movie/list?api_key=1ee04cdd24bdc8497ec43f739fd3b5a5&language=en-US
    @GET("genre/movie/list?language=en-US")
    fun getGenre(@Query("api_key") api_key: String): Call<Genres>

    // Get Video Key
    // movie/76600/videos?api_key=1ee04cdd24bdc8497ec43f739fd3b5a5&language=en-US
    @GET("movie/{movie_id}}/videos?language=en-US")
    fun getVideo(@Path("movie_id") movie_id: Int, @Query("api_key") api_key: String): Call<Videos>
}