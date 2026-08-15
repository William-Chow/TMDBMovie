package com.movielist.tmdb

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import coil.compose.AsyncImage
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.movielist.tmdb.network.MovieApi
import com.movielist.tmdb.network.RetrofitClient
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.ui.theme.TMDBMovieTheme
import com.movielist.tmdb.util.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MovieActivity : ComponentActivity() {

    private var mInterstitialAd: InterstitialAd? = null
    private var interstitialShown = false
    private lateinit var movieAPI: MovieApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        movieAPI = RetrofitClient.getClient().create(MovieApi::class.java)
        val movieID = intent?.getIntExtra("movie", 0) ?: 0

        // Survives configuration changes and process death, so a recreated
        // activity never shows a second interstitial for the same visit.
        interstitialShown = savedInstanceState?.getBoolean(STATE_INTERSTITIAL_SHOWN) == true

        initAdmob()
        if (!interstitialShown) {
            loadInterstitial()
        }

        setContent {
            TMDBMovieTheme {
                MovieDetailScreen(movieID)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_INTERSTITIAL_SHOWN, interstitialShown)
    }

    override fun onResume() {
        super.onResume()
        // The ad may have finished loading while we were paused.
        showInterstitialIfNeeded()
    }

    private fun initAdmob() {
        MobileAds.initialize(this) { }
    }

    private fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, getString(R.string.admob_interstitial_ad_unit_id), adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    // Only show from a live, resumed activity; a callback that
                    // arrives while this instance is being torn down for a
                    // configuration change is left for the new instance.
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        showInterstitialIfNeeded()
                    }
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }
            })
    }

    private fun showInterstitialIfNeeded() {
        if (interstitialShown) return
        val ad = mInterstitialAd ?: return
        interstitialShown = true
        mInterstitialAd = null
        ad.show(this)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MovieDetailScreen(movieID: Int) {
        var movie by remember { mutableStateOf<Movie?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(movieID) {
            if (movieID != 0 && Utils.checkInternetConnection(this@MovieActivity)) {
                movieAPI.getMovie(movieID, RetrofitClient.API_KEY).enqueue(object : Callback<Movie> {
                    override fun onResponse(call: Call<Movie>, response: Response<Movie>) {
                        movie = response.body()
                        isLoading = false
                    }
                    override fun onFailure(call: Call<Movie>, t: Throwable) {
                        isLoading = false
                        Toast.makeText(this@MovieActivity, t.message, Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(movie?.title ?: "Movie Detail") },
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    AsyncImage(
                        model = Utils.imageURL + movie?.poster_path,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = movie?.title ?: "", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "${movie?.release_date} (${movie?.release_date?.let { Utils.getYear(it) }})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = Utils.getGenres(movie?.genres),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Overview", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = movie?.overview?.ifEmpty { "-" } ?: "-",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    private companion object {
        const val STATE_INTERSTITIAL_SHOWN = "interstitial_shown"
    }
}
