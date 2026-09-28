package com.movielist.tmdb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.movielist.tmdb.ads.AdsConsentManager
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.ui.GalleryViewModel
import com.movielist.tmdb.ui.components.AdBanner
import com.movielist.tmdb.ui.components.EmptyState
import com.movielist.tmdb.ui.components.ErrorState
import com.movielist.tmdb.ui.components.LoadingState
import com.movielist.tmdb.ui.components.PageErrorRow
import com.movielist.tmdb.ui.components.PageLoadingRow
import com.movielist.tmdb.ui.components.rememberErrorMessage
import com.movielist.tmdb.ui.theme.TMDBMovieTheme
import com.movielist.tmdb.util.Utils

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
    fun GalleryScreen(viewModel: GalleryViewModel = viewModel()) {
        // Owned by the ViewModel: the grid survives rotation, the genre
        // process death as well.
        val pager = viewModel.pager
        val selectedGenre = viewModel.selectedGenre
        var isMenuExpanded by remember { mutableStateOf(false) }

        // A new genre is a new list with a scroll position of its own, kept
        // across rotation. (Resetting one shared state with scrollToItem(0)
        // before loading never returns: scrolling waits for the grid's first
        // layout, and the grid is only composed once there are movies.)
        val gridState = rememberSaveable(pager, saver = LazyGridState.Saver) { LazyGridState() }

        val lastVisibleIndex by remember(gridState) {
            derivedStateOf { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
        }
        LaunchedEffect(lastVisibleIndex, pager.movies.size) {
            if (pager.movies.isNotEmpty() &&
                lastVisibleIndex >= pager.movies.size - PREFETCH_DISTANCE
            ) {
                pager.loadNext()
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
                        onClick = {
                            isMenuExpanded = true
                            viewModel.onGenreMenuOpened()
                        },
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
                                viewModel.selectGenre(null)
                                isMenuExpanded = false
                            }
                        )
                        viewModel.genres.forEach { genre ->
                            DropdownMenuItem(
                                text = { Text(genre.name ?: "") },
                                onClick = {
                                    viewModel.selectGenre(genre)
                                    isMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                PullToRefreshBox(
                    isRefreshing = pager.isRefreshing,
                    onRefresh = { pager.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    val error = pager.error
                    when {
                        pager.isLoadingFirstPage -> LoadingState()

                        pager.movies.isEmpty() && error != null -> ErrorState(
                            message = rememberErrorMessage(error),
                            onRetry = { pager.retry() }
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
                            if (error != null) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    PageErrorRow(
                                        message = rememberErrorMessage(error),
                                        onRetry = { pager.retry() }
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
