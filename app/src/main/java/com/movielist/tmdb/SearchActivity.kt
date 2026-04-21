package com.movielist.tmdb

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.movielist.tmdb.network.MovieApi
import com.movielist.tmdb.network.RetrofitClient
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.network.model.Movies
import com.movielist.tmdb.ui.theme.TMDBMovieTheme
import com.movielist.tmdb.util.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : ComponentActivity() {

    private lateinit var movieAPI: MovieApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        initAdmob()
        movieAPI = RetrofitClient.getClient().create(MovieApi::class.java)

        setContent {
            TMDBMovieTheme {
                SearchScreen()
            }
        }
    }

    private fun initAdmob() {
        MobileAds.initialize(this) { }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SearchScreen() {
        var query by remember { mutableStateOf("") }
        var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        TextField(
                            value = query,
                            onValueChange = { 
                                query = it
                                if (it.length > 2) {
                                    searchMovies(it) { results ->
                                        movies = results
                                    }
                                } else {
                                    movies = emptyList()
                                }
                            },
                            placeholder = { Text("Search movies...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { 
                                        query = ""
                                        movies = emptyList()
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            bottomBar = {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    factory = { context ->
                        AdView(context).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = context.getString(R.string.admob_banner_ad_unit_id)
                            loadAd(AdRequest.Builder().build())
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                if (movies.isEmpty() && query.length <= 2) {
                    Text(
                        "Search for your favorite movies",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn {
                        items(movies) { movie ->
                            SearchItem(movie)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SearchItem(movie: Movie) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { Utils.intent(this@SearchActivity, movie.id, MovieActivity::class.java) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = Utils.imageURL + movie.poster_path,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = movie.title ?: "", style = MaterialTheme.typography.titleMedium)
                Text(text = movie.release_date ?: "", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    private fun searchMovies(msg: String, onResult: (List<Movie>) -> Unit) {
        if (!Utils.checkInternetConnection(this)) {
            Toast.makeText(this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
            return
        }
        
        movieAPI.getSearch(RetrofitClient.API_KEY, msg, 1).enqueue(object : Callback<Movies> {
            override fun onResponse(call: Call<Movies>, response: Response<Movies>) {
                onResult(response.body()?.results ?: emptyList())
            }

            override fun onFailure(call: Call<Movies>, t: Throwable) {
                Toast.makeText(this@SearchActivity, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
}
