package com.movielist.tmdb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.movielist.tmdb.ads.AdsConsentManager
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.ui.SearchViewModel
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
private const val PREFETCH_DISTANCE = 5

class SearchActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AdsConsentManager.refresh(this)

        setContent {
            TMDBMovieTheme {
                SearchScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SearchScreen(viewModel: SearchViewModel = viewModel()) {
        // Owned by the ViewModel, which also debounces typing: results survive
        // rotation, the typed query process death as well.
        val pager = viewModel.pager
        val submittedQuery = viewModel.submittedQuery

        // A new term is a new list with a scroll position of its own, kept
        // across rotation. (Resetting one shared state with scrollToItem(0)
        // before loading never returns: scrolling waits for the list's first
        // layout, and the list is only composed once there are results.)
        val listState = rememberSaveable(pager, saver = LazyListState.Saver) { LazyListState() }

        val lastVisibleIndex by remember(listState) {
            derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
        }
        LaunchedEffect(lastVisibleIndex, pager.movies.size) {
            if (pager.movies.isNotEmpty() &&
                lastVisibleIndex >= pager.movies.size - PREFETCH_DISTANCE
            ) {
                pager.loadNext()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        TextField(
                            value = viewModel.query,
                            onValueChange = viewModel::onQueryChange,
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (viewModel.query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = stringResource(R.string.clear)
                                        )
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
            },
            bottomBar = { AdBanner() }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                val error = pager.error
                when {
                    submittedQuery.isEmpty() -> Text(
                        text = stringResource(R.string.search_prompt),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    pager.isLoadingFirstPage -> LoadingState()

                    pager.movies.isEmpty() && error != null -> ErrorState(
                        message = rememberErrorMessage(error),
                        onRetry = { pager.retry() }
                    )

                    pager.movies.isEmpty() -> EmptyState(
                        stringResource(R.string.search_no_results, submittedQuery)
                    )

                    else -> LazyColumn(state = listState) {
                        items(pager.movies) { movie ->
                            SearchItem(movie)
                        }
                        if (pager.isLoading) {
                            item { PageLoadingRow() }
                        }
                        if (error != null) {
                            item {
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

    @Composable
    fun SearchItem(movie: Movie) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { Utils.intent(this@SearchActivity, movie.id, MovieActivity::class.java) }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = Utils.imageURL + movie.poster_path,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_no_exist),
                error = painterResource(R.drawable.ic_no_exist)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = movie.title ?: "", style = MaterialTheme.typography.titleMedium)
                Text(text = movie.release_date ?: "", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
