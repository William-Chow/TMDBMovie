package com.movielist.tmdb.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.movielist.tmdb.R
import com.movielist.tmdb.util.Utils

/**
 * A poster or profile picture from TMDB's image server, [size] being one of
 * the widths in [Utils] (posterSmall and so on).
 *
 * While it loads it shows a plain surface, so a slow image is not mistaken
 * for a missing one; the "no image" art is kept for images that failed and
 * for movies and people that have none, which are never requested at all.
 */
@Composable
fun TmdbImage(
    path: String?,
    size: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val loadingColor = MaterialTheme.colorScheme.surfaceVariant
    AsyncImage(
        model = Utils.imageURL(path, size),
        contentDescription = null,
        modifier = modifier,
        placeholder = remember(loadingColor) { ColorPainter(loadingColor) },
        // Coil also shows this for a null model (no image path).
        error = painterResource(R.drawable.ic_no_exist),
        contentScale = contentScale
    )
}
