package com.movielist.tmdb

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.android.gms.ads.*
import com.movielist.tmdb.network.MovieApi
import com.movielist.tmdb.network.RetrofitClient
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.network.model.Movies
import com.movielist.tmdb.ui.theme.TMDBMovieTheme
import com.movielist.tmdb.util.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {

    private lateinit var movieAPI: MovieApi
    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initAdmob()
        movieAPI = RetrofitClient.getClient().create(MovieApi::class.java)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedTime + 3000 > System.currentTimeMillis()) {
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, "Press back again to leave the app.", Toast.LENGTH_LONG).show()
                    backPressedTime = System.currentTimeMillis()
                }
            }
        })

        setContent {
            TMDBMovieTheme {
                MainScreen()
            }
        }
    }

    private fun initAdmob() {
        MobileAds.initialize(this) { }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            if (Utils.checkInternetConnection(this@MainActivity)) {
                movieAPI.getDiscover(RetrofitClient.API_KEY, 1).enqueue(object : Callback<Movies> {
                    override fun onResponse(call: Call<Movies>, response: Response<Movies>) {
                        movies = response.body()?.results ?: emptyList()
                        isLoading = false
                    }

                    override fun onFailure(call: Call<Movies>, t: Throwable) {
                        isLoading = false
                        Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                isLoading = false
                Toast.makeText(this@MainActivity, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("TMDB Movie") },
                    actions = {
                        IconButton(onClick = { Utils.intent(this@MainActivity, SearchActivity::class.java) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            },
            bottomBar = {
                Column {
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
                    Button(
                        onClick = { Utils.intent(this@MainActivity, GalleryActivity::class.java) },
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text("GALLERY VIEW")
                    }
                }
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { movies.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.padding(paddingValues).fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 32.dp)
                ) { page ->
                    val movie = movies[page]
                    MovieCard(movie)
                }
            }
        }
    }

    @Composable
    fun MovieCard(movie: Movie) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize()
                .clickable { Utils.intent(this@MainActivity, movie.id, MovieActivity::class.java) },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = Utils.imageURL + movie.poster_path,
                    contentDescription = null,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = movie.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
