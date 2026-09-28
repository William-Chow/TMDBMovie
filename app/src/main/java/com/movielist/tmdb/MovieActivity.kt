package com.movielist.tmdb

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.movielist.tmdb.ads.AdsConsentManager
import com.movielist.tmdb.ads.InterstitialPacer
import com.movielist.tmdb.data.FavoritesStore
import com.movielist.tmdb.network.model.Cast
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.ui.MovieViewModel
import com.movielist.tmdb.ui.components.AdBottomBar
import com.movielist.tmdb.ui.components.ErrorState
import com.movielist.tmdb.ui.components.LoadingState
import com.movielist.tmdb.ui.components.TmdbImage
import com.movielist.tmdb.ui.components.rememberErrorMessage
import com.movielist.tmdb.ui.theme.TMDBMovieTheme
import com.movielist.tmdb.util.Utils

class MovieActivity : ComponentActivity() {

    private var mInterstitialAd: InterstitialAd? = null
    private var interstitialShown = false
    private var finishAfterInterstitial = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Survives configuration changes and process death, so a recreated
        // activity never shows a second interstitial for the same visit.
        interstitialShown = savedInstanceState?.getBoolean(STATE_INTERSTITIAL_SHOWN) == true
        finishAfterInterstitial =
            savedInstanceState?.getBoolean(STATE_FINISH_AFTER_INTERSTITIAL) == true

        if (savedInstanceState == null) {
            // One visit per screen opened, not per rotation.
            InterstitialPacer.shared.onDetailVisited()
        }

        AdsConsentManager.refresh(this)
        // Consent was gathered by the launcher activity; without it no ad is
        // requested at all. Nor is one loaded that pacing would not let show.
        if (!interstitialShown && AdsConsentManager.canRequestAds &&
            InterstitialPacer.shared.isAdDue(SystemClock.elapsedRealtime())
        ) {
            loadInterstitial()
        }

        // The ad is shown on the way out, so the system back gesture has to
        // take the same path as the toolbar back button.
        onBackPressedDispatcher.addCallback(this) { leaveScreen() }

        setContent {
            TMDBMovieTheme {
                MovieDetailScreen()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_INTERSTITIAL_SHOWN, interstitialShown)
        outState.putBoolean(STATE_FINISH_AFTER_INTERSTITIAL, finishAfterInterstitial)
    }

    override fun onResume() {
        super.onResume()
        // Resumed after the interstitial was dismissed — covers the case where
        // a configuration change replaced the instance that showed it, leaving
        // its fullScreenContentCallback pointing at a dead activity.
        if (finishAfterInterstitial && !isFinishing) {
            finish()
        }
    }

    private fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, getString(R.string.admob_interstitial_ad_unit_id), adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    // Held until the user leaves the screen; see leaveScreen().
                    mInterstitialAd = interstitialAd
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }
            })
    }

    /**
     * Exits the screen, showing the interstitial on the way out if one is
     * ready and [InterstitialPacer] allows it. Leaving is a deliberate
     * transition, so the ad never interrupts the user mid-read.
     */
    private fun leaveScreen() {
        val ad = mInterstitialAd
        if (interstitialShown || ad == null ||
            !InterstitialPacer.shared.isAdDue(SystemClock.elapsedRealtime())
        ) {
            // Never hold the user on the screen waiting for an ad to load.
            finish()
            return
        }
        interstitialShown = true
        finishAfterInterstitial = true
        mInterstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() =
                InterstitialPacer.shared.onAdShown(SystemClock.elapsedRealtime())
            override fun onAdDismissedFullScreenContent() = finish()
            override fun onAdFailedToShowFullScreenContent(adError: AdError) = finish()
        }
        ad.show(this)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MovieDetailScreen(viewModel: MovieViewModel = viewModel()) {
        // Owned by the ViewModel, so a rotation keeps the loaded movie, and
        // the movie id (an intent extra) brings it back after process death.
        val movie = viewModel.movie
        val error = viewModel.error

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(movie?.title ?: stringResource(R.string.movie_detail)) },
                    navigationIcon = {
                        IconButton(onClick = { leaveScreen() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        movie?.let { loaded ->
                            val isFavorite = FavoritesStore.isFavorite(loaded.id)
                            IconButton(onClick = { FavoritesStore.toggle(loaded) }) {
                                Icon(
                                    imageVector = if (isFavorite) {
                                        Icons.Default.Favorite
                                    } else {
                                        Icons.Default.FavoriteBorder
                                    },
                                    contentDescription = stringResource(
                                        if (isFavorite) {
                                            R.string.remove_from_favorites
                                        } else {
                                            R.string.add_to_favorites
                                        }
                                    )
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = { AdBottomBar() }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when {
                    // Nothing to load; say so rather than spinning forever.
                    viewModel.isUnavailable -> ErrorState(
                        message = stringResource(R.string.error_movie_unavailable),
                        onRetry = { viewModel.retry() }
                    )

                    error != null -> ErrorState(
                        message = rememberErrorMessage(error),
                        onRetry = { viewModel.retry() }
                    )

                    movie == null -> LoadingState()

                    else -> MovieDetail(movie, viewModel.trailerKey, viewModel.cast)
                }
            }
        }
    }

    @Composable
    private fun MovieDetail(movie: Movie, trailerKey: String?, cast: List<Cast>) {
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TmdbImage(
                path = movie.poster_path,
                size = Utils.posterLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = movie.title ?: "", style = MaterialTheme.typography.headlineMedium)

            movie.tagline?.takeIf { it.isNotBlank() }?.let { tagline ->
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            movie.release_date?.takeIf { it.isNotBlank() }?.let { releaseDate ->
                Text(
                    text = stringResource(
                        R.string.release_and_year,
                        releaseDate,
                        Utils.getYear(releaseDate)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Utils.formatRuntime(movie.runtime)?.let { runtime ->
                Text(text = runtime, style = MaterialTheme.typography.bodyMedium)
            }

            movie.vote_average?.takeIf { it > 0.0 }?.let { rating ->
                val votes = movie.vote_count ?: 0
                Text(
                    text = pluralStringResource(R.plurals.rating_votes, votes, rating, votes),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = Utils.getGenres(movie.genres),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (trailerKey != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { Utils.openUrl(context, Utils.youtubeURL + trailerKey) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.watch_trailer))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = stringResource(R.string.overview), style = MaterialTheme.typography.titleLarge)
            Text(
                text = movie.overview?.takeIf { it.isNotBlank() } ?: stringResource(R.string.not_available),
                style = MaterialTheme.typography.bodyMedium
            )

            if (cast.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.cast), style = MaterialTheme.typography.titleLarge)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(cast) { member -> CastMember(member) }
                }
            }
        }
    }

    @Composable
    private fun CastMember(member: Cast) {
        Column(
            modifier = Modifier.width(88.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TmdbImage(
                path = member.profile_path,
                size = Utils.profileSmall,
                modifier = Modifier.size(72.dp).clip(CircleShape)
            )
            Text(
                text = member.name ?: "",
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            member.character?.takeIf { it.isNotBlank() }?.let { character ->
                Text(
                    text = character,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    private companion object {
        const val STATE_INTERSTITIAL_SHOWN = "interstitial_shown"
        const val STATE_FINISH_AFTER_INTERSTITIAL = "finish_after_interstitial"
    }
}
