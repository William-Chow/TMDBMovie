package com.movielist.tmdb.util

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import com.movielist.tmdb.R
import com.movielist.tmdb.network.MissingApiKeyException
import com.movielist.tmdb.network.isUnsatisfiedCacheOnlyResponse
import com.movielist.tmdb.network.model.Genre
import com.movielist.tmdb.network.model.Video
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*


class Utils {

    companion object {
        const val imageURL = "https://image.tmdb.org/t/p/w500"
        const val profileImageURL = "https://image.tmdb.org/t/p/w185"
        const val youtubeURL = "https://www.youtube.com/watch?v="
        const val tmdbURL = "https://www.themoviedb.org/"

        /**
         * Whether the device has a network that claims internet access. Only
         * used to word an error: requests always try the network, so one that
         * has not been validated (yet) still gets used.
         */
        fun checkInternetConnection(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            // Asking for the capability rather than the transport also covers
            // Ethernet (emulators) and VPN, which the transport list missed.
            return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        /** Turns a request failure into something worth showing the user. */
        fun errorMessage(context: Context, throwable: Throwable): String = when (throwable) {
            // A build without a key: say what to fix, not "please try again".
            is MissingApiKeyException -> context.getString(R.string.error_missing_api_key)
            // A dropped request means something different depending on whether
            // the device has a network at all.
            is IOException -> context.getString(
                if (checkInternetConnection(context)) {
                    R.string.error_unreachable
                } else {
                    R.string.no_internet_connection
                }
            )
            is HttpException -> {
                val raw = throwable.response()?.raw()
                if (raw != null && isUnsatisfiedCacheOnlyResponse(raw)) {
                    // OkHttp's own 504 for "cache only, and nothing cached":
                    // no server answered, the device is offline.
                    context.getString(R.string.no_internet_connection)
                } else {
                    context.getString(R.string.error_server, throwable.code())
                }
            }
            else -> throwable.localizedMessage ?: context.getString(R.string.error_unknown)
        }

        @SuppressLint("SimpleDateFormat")
        fun getYear(date: String): Int {
            if(date.isNotEmpty()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd")
                val parse: Date? = sdf.parse(date)
                val c: Calendar = Calendar.getInstance()
                if (parse != null) {
                    c.time = parse
                }
                return c.get(Calendar.YEAR)
            }
            return 0
        }

        /** Today in the device's time zone, in the yyyy-MM-dd form TMDB's date filters take. */
        fun today(): String = LocalDate.now().toString()

        /**
         * Languages to ask TMDB for trailers in: English, clips with no
         * language set ("null", how many trailers are filed) and the film's
         * [originalLanguage], without which many non-English films have none.
         */
        fun videoLanguages(originalLanguage: String?): String {
            val original = originalLanguage?.trim()?.lowercase()
            return if (original.isNullOrEmpty() || original == "en" || original == "null") {
                "en,null"
            } else {
                "en,null,$original"
            }
        }

        /**
         * Picks the most trailer-like YouTube clip TMDB reported, if any: an
         * official trailer before any trailer, then a teaser, then any clip.
         * Among equals English comes first, then untagged clips, then others.
         */
        fun pickTrailerKey(videos: List<Video>?): String? {
            val youtube = videos.orEmpty()
                .filter { it.site.equals("YouTube", ignoreCase = true) && !it.key.isNullOrBlank() }
                .sortedBy {
                    when (it.iso_639_1?.lowercase()) {
                        "en" -> 0
                        null, "", "xx" -> 1
                        else -> 2
                    }
                }
            val trailers = youtube.filter { it.type.equals("Trailer", ignoreCase = true) }
            return (trailers.firstOrNull { it.official == true }
                ?: trailers.firstOrNull()
                ?: youtube.firstOrNull { it.type.equals("Teaser", ignoreCase = true) }
                ?: youtube.firstOrNull())?.key
        }

        fun getGenres(genres: List<Genre>?): String =
            genres.orEmpty().mapNotNull { it.name }.joinToString(", ")

        /** "1h 47m", or null when the API did not report a runtime. */
        fun formatRuntime(minutes: Int?): String? {
            if (minutes == null || minutes <= 0) return null
            val hours = minutes / 60
            val remainder = minutes % 60
            return if (hours > 0) "${hours}h ${remainder}m" else "${remainder}m"
        }

        fun intent(context: Context, movieID: Int?, className: Class<*>?) {
            val intent = Intent(context, className)
            intent.putExtra("movie", movieID)
            context.startActivity(intent)
        }

        fun intent(context: Context, className: Class<*>?) {
            val intent = Intent(context, className)
            context.startActivity(intent)
        }

        /** Hands a URL to the browser / YouTube app, if the device has one. */
        fun openUrl(context: Context, url: String) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.error_no_browser, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
