package com.movielist.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.movielist.network.model.Genre
import java.text.SimpleDateFormat
import java.time.format.*
import java.util.*
import java.util.stream.Collectors

class Utils {

    companion object {
        const val imageURL = "https://image.tmdb.org/t/p/w500"
        const val youtubeURL = "https://www.youtube.com/watch?v="

        // Check Internet Connection
        fun checkInternetConnection(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        }

        @SuppressLint("SimpleDateFormat")
        fun getYear(date: String): Int {
            val sdf = SimpleDateFormat("yyyy-MM-dd")
            val parse: Date? = sdf.parse(date)
            val c: Calendar = Calendar.getInstance()
            if (parse != null) {
                c.time = parse
            }
            return c.get(Calendar.YEAR)
        }

        fun getGenres(genres: List<Genre>?): String {
            if (null != genres) {
                return genres.stream().map { genre: Genre -> genre.name }
                    .collect(Collectors.joining(", "))
            }
            return ""
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
    }
}