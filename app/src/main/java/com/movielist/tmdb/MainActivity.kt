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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movielist.tmdb.ads.AdsConsentManager
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.ui.MainViewModel
import com.movielist.tmdb.ui.MoviePager
import com.movielist.tmdb.ui.components.AdBanner
import com.movielist.tmdb.ui.components.EmptyState
import com.movielist.tmdb.ui.components.ErrorState
import com.movielist.tmdb.ui.components.LoadingState
import com.movielist.tmdb.ui.components.PullableContent
import com.movielist.tmdb.ui.components.TmdbImage
import com.movielist.tmdb.ui.components.rememberErrorMessage
import com.movielist.tmdb.ui.theme.TMDBMovieTheme
import com.movielist.tmdb.util.Utils

/** Pages left ahead of the user before the next page is requested. */
private const val PREFETCH_DISTANCE = 3

class MainActivity : ComponentActivity() {

    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launcher activity, so this is where the app asks for ad consent.
        // Nothing is requested from AdMob until that comes back positive.
        AdsConsentManager.gatherConsent(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedTime + 3000 > System.currentTimeMillis()) {
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, R.string.press_back_again, Toast.LENGTH_LONG).show()
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen(viewModel: MainViewModel = viewModel()) {
        val context = LocalContext.current
        // Owned by the ViewModel, so the carousel survives rotation.
        val pager = viewModel.pager

        // A page that fails after the first one leaves the loaded movies on
        // screen, so the failure is reported without replacing them. Clearing
        // it afterwards matters: the pager refuses to load while an error is
        // pending, and in a carousel there is no error row to retry from.
        LaunchedEffect(pager.error) {
            val error = pager.error
            if (error != null && pager.movies.isNotEmpty()) {
                Toast.makeText(context, Utils.errorMessage(context, error), Toast.LENGTH_SHORT).show()
                pager.dismissError()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = { Utils.intent(this@MainActivity, FavoritesActivity::class.java) }) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.favorites)
                            )
                        }
                        IconButton(onClick = { Utils.intent(this@MainActivity, SearchActivity::class.java) }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                        }
                        // Pulling a sideways carousel down is easy to miss (and
                        // not reachable for everyone), so refresh has a button too.
                        IconButton(onClick = { pager.refresh() }, enabled = !pager.isRefreshing) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                        OverflowMenu()
                    }
                )
            },
            bottomBar = {
                // Like AdBottomBar: Scaffold does not inset the bars it is given.
                Column(modifier = Modifier.navigationBarsPadding()) {
                    AdBanner()
                    Button(
                        onClick = { Utils.intent(this@MainActivity, GalleryActivity::class.java) },
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text(stringResource(R.string.gallery_view))
                    }
                }
            }
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = pager.isRefreshing,
                onRefresh = { pager.refresh() },
                modifier = Modifier.padding(paddingValues).fillMaxSize()
            ) {
                val error = pager.error
                when {
                    pager.isLoadingFirstPage -> LoadingState()

                    pager.movies.isEmpty() && error != null -> ErrorState(
                        message = rememberErrorMessage(error),
                        onRetry = { pager.retry() }
                    )

                    pager.movies.isEmpty() -> EmptyState(stringResource(R.string.no_movies))

                    // The carousel only scrolls sideways; PullableContent is
                    // what lets a downward drag reach the PullToRefreshBox.
                    else -> PullableContent { MovieCarousel(pager) }
                }
            }
        }
    }

    /** About, and the ad consent choices for users who must be able to change them. */
    @Composable
    fun OverflowMenu() {
        var isExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { isExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
            }
            DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                if (AdsConsentManager.isPrivacyOptionsRequired) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.privacy_settings)) },
                        onClick = {
                            isExpanded = false
                            AdsConsentManager.showPrivacyOptionsForm(this@MainActivity)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.about)) },
                    onClick = {
                        isExpanded = false
                        Utils.intent(this@MainActivity, AboutActivity::class.java)
                    }
                )
            }
        }
    }

    @Composable
    fun MovieCarousel(pager: MoviePager) {
        // A refreshed list is a new list, so it starts again from its first
        // movie instead of wherever the old one had been swiped to.
        key(pager.refreshCount) {
            val pagerState = rememberPagerState(pageCount = { pager.movies.size })

            // Re-evaluated whenever the user swipes or a page arrives, so the
            // list keeps extending as long as the user keeps going.
            LaunchedEffect(pagerState.currentPage, pager.movies.size) {
                if (pagerState.currentPage >= pager.movies.size - PREFETCH_DISTANCE) {
                    pager.loadNext()
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 32.dp)
            ) { page ->
                MovieCard(pager.movies[page])
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
                TmdbImage(
                    path = movie.poster_path,
                    size = Utils.posterLarge,
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
