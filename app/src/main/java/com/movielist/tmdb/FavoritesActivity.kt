package com.movielist.tmdb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.movielist.tmdb.ads.AdsConsentManager
import com.movielist.tmdb.data.FavoriteMovie
import com.movielist.tmdb.data.FavoritesStore
import com.movielist.tmdb.ui.components.AdBanner
import com.movielist.tmdb.ui.components.EmptyState
import com.movielist.tmdb.ui.theme.TMDBMovieTheme
import com.movielist.tmdb.util.Utils

/**
 * The movies the user saved from the detail screen. Reads straight from
 * [FavoritesStore], so removing a favourite anywhere updates this grid.
 */
class FavoritesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AdsConsentManager.refresh(this)

        setContent {
            TMDBMovieTheme {
                FavoritesScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FavoritesScreen() {
        val favorites = FavoritesStore.favorites

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.favorites)) },
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
                if (favorites.isEmpty()) {
                    EmptyState(stringResource(R.string.no_favorites))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(favorites) { movie ->
                            FavoriteItem(movie)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun FavoriteItem(movie: FavoriteMovie) {
        Card(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .clickable { Utils.intent(this@FavoritesActivity, movie.id, MovieActivity::class.java) },
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
