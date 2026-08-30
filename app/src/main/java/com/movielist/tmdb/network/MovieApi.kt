package com.movielist.tmdb.network

import androidx.annotation.Keep
import com.movielist.tmdb.network.model.Credits
import com.movielist.tmdb.network.model.Genres
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.network.model.Movies
import com.movielist.tmdb.network.model.Videos
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Keep
interface MovieApi {

    // Get Movie List
    // discover/movie?api_key={api_key}&page=1&with_genres=28
    @GET("discover/movie?sort_by=release_date.desc&include_adult=false&include_video=true")
    suspend fun getDiscover(
        @Query("api_key") api_key: String,
        @Query("page") page: Int,
        // Omitted from the query string when null, which asks for every genre.
        @Query("with_genres") with_genres: Int?
    ): Movies

    // Get Movie Item
    // movie/76600?api_key={api_key}&language=en-US
    @GET("movie/{movie_id}?language=en-US")
    suspend fun getMovie(@Path("movie_id") movie_id: Int, @Query("api_key") api_key: String): Movie

    // Get Search
    // search/movie?api_key={api_key}&language=en-US
    @GET("search/movie?language=en-US")
    suspend fun getSearch(
        @Query("api_key") api_key: String,
        @Query("query") query: String,
        @Query("page") page: Int
    ): Movies

    // Get Genre
    // genre/movie/list?api_key={api_key}&language=en-US
    @GET("genre/movie/list?language=en-US")
    suspend fun getGenre(@Query("api_key") api_key: String): Genres

    // Get Cast & Crew
    // movie/76600/credits?api_key={api_key}&language=en-US
    @GET("movie/{movie_id}/credits?language=en-US")
    suspend fun getCredits(@Path("movie_id") movie_id: Int, @Query("api_key") api_key: String): Credits

    // Get Video Key
    // movie/76600/videos?api_key={api_key}&language=en-US
    @GET("movie/{movie_id}/videos?language=en-US")
    suspend fun getVideo(@Path("movie_id") movie_id: Int, @Query("api_key") api_key: String): Videos
}
