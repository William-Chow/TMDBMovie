package com.movielist.tmdb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movielist.tmdb.ads.AdsConsentManager
import com.movielist.tmdb.ui.theme.TMDBMovieTheme
import com.movielist.tmdb.util.Utils

/**
 * Version, the attribution TMDB's API terms ask for, and the way back into
 * the ad consent choices for users who have to be offered one.
 */
class AboutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AdsConsentManager.refresh(this)

        setContent {
            TMDBMovieTheme {
                AboutScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AboutScreen() {
        val context = LocalContext.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.about)) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                Text(
                    text = stringResource(R.string.about_data_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.tmdb_attribution),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = { Utils.openUrl(context, Utils.tmdbURL) }) {
                    Text(stringResource(R.string.tmdb_website))
                }

                if (AdsConsentManager.isPrivacyOptionsRequired) {
                    HorizontalDivider()

                    Text(
                        text = stringResource(R.string.privacy_settings),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.privacy_settings_summary),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = { AdsConsentManager.showPrivacyOptionsForm(this@AboutActivity) }
                    ) {
                        Text(stringResource(R.string.privacy_settings_open))
                    }
                }
            }
        }
    }
}
