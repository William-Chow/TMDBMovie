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
import com.movielist.tmdb.network.model.Genre
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class Utils {

    companion object {
        const val imageURL = "https://image.tmdb.org/t/p/w500"
        const val profileImageURL = "https://image.tmdb.org/t/p/w185"
        const val youtubeURL = "https://www.youtube.com/watch?v="

        // Check Internet Connection
        fun checkInternetConnection(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            // Asking for the capability rather than the transport also covers
            // Ethernet (emulators) and VPN, which the transport list missed.
            return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        /** Turns a request failure into something worth showing the user. */
        fun errorMessage(context: Context, throwable: Throwable): String = when (throwable) {
            // A dropped request means something different depending on whether
            // the device has a network at all.
            is IOException -> context.getString(
                if (checkInternetConnection(context)) {
                    R.string.error_unreachable
                } else {
                    R.string.no_internet_connection
                }
            )
            is HttpException -> context.getString(R.string.error_server, throwable.code())
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
