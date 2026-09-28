package com.movielist.tmdb.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.movielist.tmdb.R
import com.movielist.tmdb.ads.AdsConsentManager
import com.movielist.tmdb.util.Utils

/**
 * A centred column at least as tall as the space it is given, inside a
 * vertical scroll. PullToRefreshBox only reacts to nested scrolling, so
 * without the scroll a loading, empty or error state could not be pulled;
 * content taller than the screen (landscape, large fonts) simply scrolls.
 */
@Composable
private fun ScrollableCenteredColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val minHeight = if (constraints.hasBoundedHeight) maxHeight else 0.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/**
 * Lays [content] out in exactly the space this is given, inside a vertical
 * scroll that has nothing to scroll. Content that only scrolls sideways, like
 * the home carousel, never produces the vertical nested scrolling a
 * PullToRefreshBox listens for; this scroll turns a downward drag into it.
 */
@Composable
fun PullableContent(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewport = if (constraints.hasBoundedHeight) Modifier.height(maxHeight) else Modifier
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .then(viewport),
            content = content
        )
    }
}

/**
 * The message to show for a failed request. Worked out once per failure,
 * while its connectivity check still describes the moment it failed.
 */
@Composable
fun rememberErrorMessage(error: Throwable): String {
    val context = LocalContext.current
    return remember(error) { Utils.errorMessage(context, error) }
}

/** Full-screen spinner, for the first load of a screen. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    ScrollableCenteredColumn(modifier) {
        CircularProgressIndicator()
    }
}

/**
 * Shown instead of content when a request failed. Always offers a way back —
 * a dead end with only a spinner is what this replaces.
 */
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    ScrollableCenteredColumn(modifier) {
        Image(
            painter = painterResource(R.drawable.ic_no_exist),
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

/** Shown when a request succeeded but there is nothing to list. */
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    ScrollableCenteredColumn(modifier) {
        Image(
            painter = painterResource(R.drawable.ic_empty_result),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

/** Row-height spinner appended below a list while the next page loads. */
@Composable
fun PageLoadingRow(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
    }
}

/**
 * The AdMob banner every screen pins to the bottom. Renders nothing until the
 * consent flow says this user may be served ads.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    if (!AdsConsentManager.canRequestAds) return
    AndroidView(
        modifier = modifier.fillMaxWidth().height(50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = context.getString(R.string.admob_banner_ad_unit_id)
                loadAd(AdRequest.Builder().build())
            }
        },
        // Leaving composition (the screen closed, or consent went away) is the
        // end of this banner: stop its refreshes and free its WebView.
        onRelease = { adView -> adView.destroy() }
    )
}

/**
 * [AdBanner] as a Scaffold bottom bar. Scaffold insets its content but not
 * the bars it is handed, and since Android 15 every app draws edge to edge,
 * so the bar has to keep clear of the navigation bar itself.
 */
@Composable
fun AdBottomBar() {
    AdBanner(Modifier.navigationBarsPadding())
}

/**
 * Inline failure notice appended below an already-populated list when the
 * *next* page fails — the list the user already has stays on screen.
 */
@Composable
fun PageErrorRow(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.retry))
        }
    }
}
