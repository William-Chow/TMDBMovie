package com.movielist.tmdb

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.android.gms.ads.*
import com.movielist.tmdb.network.MovieApi
import com.movielist.tmdb.network.RetrofitClient
import com.movielist.tmdb.network.model.Genre
import com.movielist.tmdb.network.model.Genres
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.network.model.Movies
import com.movielist.tmdb.ui.theme.TMDBMovieTheme
import com.movielist.tmdb.util.SharedPreferenceUtils
import com.movielist.tmdb.util.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GalleryActivity : ComponentActivity() {

    private lateinit var movieAPI: MovieApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        initAdmob()
        movieAPI = RetrofitClient.getClient().create(MovieApi::class.java)

        setContent {
            TMDBMovieTheme {
                GalleryScreen()
            }
        }
    }

    private fun initAdmob() {
        MobileAds.initialize(this) { }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GalleryScreen() {
        var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
        var filteredMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
        var genres by remember { mutableStateOf<List<Genre>>(emptyList()) }
        var selectedGenre by remember { mutableStateOf("All") }
        var isLoading by remember { mutableStateOf(true) }
        var isMenuExpanded by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            fetchInitialData { fetchedMovies, fetchedGenres ->
                movies = fetchedMovies
                filteredMovies = fetchedMovies
                genres = fetchedGenres
                isLoading = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Gallery") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { Utils.intent(this@GalleryActivity, SearchActivity::class.java) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
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
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                
                // Genre Selector
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    OutlinedButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedGenre)
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("All") },
                            onClick = {
                                selectedGenre = "All"
                                filteredMovies = movies
                                isMenuExpanded = false
                            }
                        )
                        genres.forEach { genre ->
                            DropdownMenuItem(
                                text = { Text(genre.name ?: "") },
                                onClick = {
                                    selectedGenre = genre.name ?: ""
                                    filteredMovies = movies.filter { it.genre_ids?.contains(genre.id) == true }
                                    isMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredMovies) { movie ->
                            GalleryItem(movie)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun GalleryItem(movie: Movie) {
        Card(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .clickable { Utils.intent(this@GalleryActivity, movie.id, MovieActivity::class.java) },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                AsyncImage(
                    model = Utils.imageURL + movie.poster_path,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = movie.title ?: "",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    private fun fetchInitialData(onComplete: (List<Movie>, List<Genre>) -> Unit) {
        if (!Utils.checkInternetConnection(this)) return

        movieAPI.getGenre(RetrofitClient.API_KEY).enqueue(object : Callback<Genres> {
            override fun onResponse(call: Call<Genres>, response: Response<Genres>) {
                val genres = response.body()?.genres ?: emptyList()
                
                movieAPI.getDiscover(RetrofitClient.API_KEY, 1).enqueue(object : Callback<Movies> {
                    override fun onResponse(call: Call<Movies>, response: Response<Movies>) {
                        val movies = response.body()?.results ?: emptyList()
                        onComplete(movies, genres)
                    }
                    override fun onFailure(call: Call<Movies>, t: Throwable) {
                        onComplete(emptyList(), genres)
                    }
                })
            }
            override fun onFailure(call: Call<Genres>, t: Throwable) {
                onComplete(emptyList(), emptyList())
            }
        })
    }
}
