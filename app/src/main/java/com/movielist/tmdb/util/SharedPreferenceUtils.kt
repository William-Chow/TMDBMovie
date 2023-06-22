package com.movielist.tmdb.util

import android.app.Activity
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.movielist.tmdb.R
import com.movielist.tmdb.network.model.Genre
import java.lang.reflect.Type


class SharedPreferenceUtils {

    companion object {

        const val key_id_genre_list = "ori_genreList"
        const val key_Genre_list = "GenreList"

        fun saveGenreArrayList(
            _activity: Activity,
            contactArrayList: List<Genre?>?,
            key: String?
        ) {
            val prefs = _activity.applicationContext.getSharedPreferences(
                _activity.getString(R.string.app_name),
                Context.MODE_PRIVATE
            )
            val editor = prefs.edit()
            val gson = Gson()
            val json = gson.toJson(contactArrayList)
            editor.putString(key, json)
            editor.apply() // This line is IMPORTANT !!!
        }

        fun saveArrayList(
            _activity: Activity,
            contactArrayList: List<String?>?,
            key: String?
        ) {
            val prefs = _activity.applicationContext.getSharedPreferences(
                _activity.getString(R.string.app_name),
                Context.MODE_PRIVATE
            )
            val editor = prefs.edit()
            val gson = Gson()
            val json = gson.toJson(contactArrayList)
            editor.putString(key, json)
            editor.apply() // This line is IMPORTANT !!!
        }

        fun getGenreArrayList(_activity: Activity, key: String?): List<Genre>? {
            val prefs = _activity.applicationContext.getSharedPreferences(
                _activity.getString(R.string.app_name),
                Context.MODE_PRIVATE
            )
            val gson = Gson()
            val json = prefs.getString(key, null)
            val type: Type = object : TypeToken<ArrayList<Genre?>?>() {}.type
            return gson.fromJson(json, type)
        }

        fun getArrayList(_activity: Activity, key: String?): List<String>? {
            val prefs = _activity.applicationContext.getSharedPreferences(
                _activity.getString(R.string.app_name),
                Context.MODE_PRIVATE
            )
            val gson = Gson()
            val json = prefs.getString(key, null)
            val type: Type = object : TypeToken<ArrayList<String?>?>() {}.type
            return gson.fromJson(json, type)
        }
    }
}