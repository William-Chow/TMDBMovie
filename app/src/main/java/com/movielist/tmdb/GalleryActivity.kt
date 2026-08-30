package com.movielist.tmdb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.movielist.tmdb.ads.AdsConsentManager
import com.movielist.tmdb.network.RetrofitClient
import com.movielist.tmdb.network.model.Genre
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.ui.components.AdBanner
import com.movielist.tmdb.ui.components.EmptyState
import com.movielist.tmdb.ui.components.ErrorState
import com.movielist.tmdb.ui.components.LoadingState
import com.movielist.tmdb.ui.components.PageErrorRow
import com.movielist.tmdb.ui.components.PageLoadingRow
import com.movielist.tmdb.ui.rememberMoviePager
import com.movielist.tmdb.ui.theme.TMDBMovieTheme
import com.movielist.tmdb.util.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Items left below the fold before the next page is requested. */
private const val PREFETCH_DISTANCE = 6

class GalleryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AdsConsentManager.refresh(this)

        setContent {
            TMDBMovieTheme {
                GalleryScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GalleryScreen() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var genres by remember { mutableStateOf<List<Genre>>(emptyList()) }
        // null means "All" — no with_genres filter is sent at all.
        var selectedGenre by remember { mutableStateOf<Genre?>(null) }
        var isMenuExpanded by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            genres = try {
                RetrofitClient.movieApi.getGenre(RetrofitClient.API_KEY).genres.orEmpty()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // The filter is a convenience; losing it must not cost the grid.
                emptyList()
            }
        }

        // Filtering happens on the server, so a genre with few recent releases
        // still fills the grid instead of returning whatever the first page held.
        val pager = rememberMoviePager(selectedGenre?.id) { page ->
            RetrofitClient.movieApi.getDiscover(RetrofitClient.API_KEY, page, selectedGenre?.id)
        }
        val gridState = rememberLazyGridState()

        LaunchedEffect(pager) {
            // A new genre is a new list; don't leave the user mid-scroll in it.
            gridState.scrollToItem(0)
            pager.loadNext(context)
        }

        val lastVisibleIndex by remember(gridState) {
            derivedStateOf { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
        }
        LaunchedEffect(lastVisibleIndex, pager.movies.size) {
            if (pager.movies.isNotEmpty() &&
                lastVisibleIndex >= pager.movies.size - PREFETCH_DISTANCE
            ) {
                pager.loadNext(context)
            }
        }

        val selectedGenreName = selectedGenre?.name ?: stringResource(R.string.genre_all)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.gallery)) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { Utils.intent(this@GalleryActivity, SearchActivity::class.java) }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                        }
                    }
                )
            },
            bottomBar = { AdBanner() }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

                // Genre Selector
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    OutlinedButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedGenreName)
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.genre_all)) },
                            onClick = {
                                selectedGenre = null
                                isMenuExpanded = false
                            }
                        )
                        genres.forEach { genre ->
                            DropdownMenuItem(
                                text = { Text(genre.name ?: "") },
                                onClick = {
                                    selectedGenre = genre
                                    isMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                PullToRefreshBox(
                    isRefreshing = pager.isRefreshing,
                    onRefresh = { scope.launch { pager.refresh(context) } },
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        pager.isLoadingFirstPage -> LoadingState()

                        pager.movies.isEmpty() && pager.error != null -> ErrorState(
                            message = pager.error!!,
                            onRetry = { scope.launch { pager.retry(context) } }
                        )

                        pager.movies.isEmpty() -> EmptyState(
                            message = if (selectedGenre == null) {
                                stringResource(R.string.no_movies)
                            } else {
                                stringResource(R.string.no_movies_in_genre, selectedGenreName)
                            }
                        )

                        else -> LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(pager.movies) { movie ->
                                GalleryItem(movie)
                            }
                            if (pager.isLoading) {
                                item(span = { GridItemSpan(maxLineSpan) }) { PageLoadingRow() }
                            }
                            pager.error?.let { message ->
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    PageErrorRow(
                                        message = message,
                                        onRetry = { scope.launch { pager.retry(context) } }
                                    )
                                }
                            }
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
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.ic_no_exist),
                    error = painterResource(R.drawable.ic_no_exist)
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
}
