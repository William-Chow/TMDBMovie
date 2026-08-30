package com.movielist.tmdb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.movielist.tmdb.ads.AdsConsentManager
import com.movielist.tmdb.network.RetrofitClient
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Quiet period after the last keystroke before a search is actually sent. */
private const val SEARCH_DEBOUNCE_MS = 350L
private const val MIN_QUERY_LENGTH = 2
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
    fun SearchScreen() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var query by remember { mutableStateOf("") }
        // The query that has actually been sent. Typing restarts the effect
        // below, so only a term the user paused on reaches the network.
        var submittedQuery by remember { mutableStateOf("") }

        LaunchedEffect(query) {
            val trimmed = query.trim()
            if (trimmed.length < MIN_QUERY_LENGTH) {
                submittedQuery = ""
                return@LaunchedEffect
            }
            delay(SEARCH_DEBOUNCE_MS)
            submittedQuery = trimmed
        }

        // Keyed on the submitted term: a new term replaces the pager outright,
        // which cancels the previous request instead of racing it.
        val pager = rememberMoviePager(submittedQuery) { page ->
            RetrofitClient.movieApi.getSearch(RetrofitClient.API_KEY, submittedQuery, page)
        }
        val listState = rememberLazyListState()

        LaunchedEffect(pager) {
            // A new term is a new list; don't leave the user mid-scroll in it.
            listState.scrollToItem(0)
            if (submittedQuery.isNotEmpty()) pager.loadNext(context)
        }

        val lastVisibleIndex by remember(listState) {
            derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
        }
        LaunchedEffect(lastVisibleIndex, pager.movies.size) {
            if (pager.movies.isNotEmpty() &&
                lastVisibleIndex >= pager.movies.size - PREFETCH_DISTANCE
            ) {
                pager.loadNext(context)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
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
                when {
                    submittedQuery.isEmpty() -> Text(
                        text = stringResource(R.string.search_prompt),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    pager.isLoadingFirstPage -> LoadingState()

                    pager.movies.isEmpty() && pager.error != null -> ErrorState(
                        message = pager.error!!,
                        onRetry = { scope.launch { pager.retry(context) } }
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
                        pager.error?.let { message ->
                            item {
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
