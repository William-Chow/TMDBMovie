package com.movielist.tmdb

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.movielist.tmdb.adapter.GalleryAdapter
import com.movielist.tmdb.network.MovieApi
import com.movielist.tmdb.network.RetrofitClient
import com.movielist.tmdb.network.model.Genres
import com.movielist.tmdb.network.model.Movies
import com.movielist.tmdb.util.SharedPreferenceUtils
import com.movielist.tmdb.util.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class GalleryActivity : AppCompatActivity() {

    private lateinit var spinner: Spinner
    private lateinit var rvGalleryView: RecyclerView
    private lateinit var btnSwipe: Button

    private lateinit var movieAPI: MovieApi

    private var backPressedTime: Long = 0

    private var genreDisplayList: List<String>? = null
    var galleryAdapter: GalleryAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        spinner = findViewById(R.id.spinner)
        rvGalleryView = findViewById(R.id.rvGalleryView)
        btnSwipe = findViewById(R.id.btnSwipe)

        btnSwipe.setOnClickListener {
            Utils.intent(this@GalleryActivity, MainActivity::class.java)
        }

        movieAPI = RetrofitClient.getClient().create(MovieApi::class.java)
        if (Utils.checkInternetConnection(this@GalleryActivity)) {
            // Get from Shared Preference
            genreDisplayList =
                SharedPreferenceUtils.getArrayList(
                    this@GalleryActivity,
                    SharedPreferenceUtils.key_Genre_list
                )
            if (null == genreDisplayList) getGenre() else genreDisplayList?.let {
                implementSpinner(
                    it
                )
            }
            getMovies()
        } else {
            Toast.makeText(
                this@GalleryActivity,
                this@GalleryActivity.getString(R.string.no_internet_connection),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getMovies() {
        movieAPI.getDiscover(RetrofitClient.API_KEY, 1).enqueue(object : Callback<Movies> {
            override fun onResponse(call: Call<Movies>, response: Response<Movies>) {
                //val statusCode = response.code()
                val movies: Movies? = response.body()
                if (movies?.results != null) {
                    val results = movies.results
                    if (results?.size!! > 0) {
                        val layoutManager = GridLayoutManager(this@GalleryActivity, 2)
                        rvGalleryView.layoutManager = layoutManager
                        galleryAdapter = GalleryAdapter(results, this@GalleryActivity)
                        galleryAdapter?.saveList(results)
                        rvGalleryView.adapter = galleryAdapter
                    }
                }
            }

            override fun onFailure(call: Call<Movies>, t: Throwable) {
                Toast.makeText(this@GalleryActivity, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getGenre() {
        movieAPI.getGenre(RetrofitClient.API_KEY).enqueue(object : Callback<Genres> {
            override fun onResponse(call: Call<Genres>, response: Response<Genres>) {
                //val statusCode = response.code()
                val genres: Genres? = response.body()
                if (null != genres) {
                    val results = genres.genres
                    if (results?.size!! > 0) {
                        // Save ID and Name
                        SharedPreferenceUtils.saveGenreArrayList(
                            this@GalleryActivity,
                            results,
                            SharedPreferenceUtils.key_id_genre_list
                        )
                        genreDisplayList = Utils.generateGenresArrayString(results)
                        // Store into Shared Preference
                        SharedPreferenceUtils.saveArrayList(
                            this@GalleryActivity,
                            genreDisplayList,
                            SharedPreferenceUtils.key_Genre_list
                        )
                        genreDisplayList?.let { implementSpinner(it) }
                    }
                }
            }

            override fun onFailure(call: Call<Genres>, t: Throwable) {
                Toast.makeText(this@GalleryActivity, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.searchView) {
            Utils.intent(this@GalleryActivity, SearchActivity::class.java)
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (backPressedTime + 3000 > System.currentTimeMillis()) {
            onBackPressedDispatcher.onBackPressed()
            finish()
        } else {
            Toast.makeText(this, "Press back again to leave the app.", Toast.LENGTH_LONG).show()
        }
        backPressedTime = System.currentTimeMillis()
    }

    private fun implementSpinner(genreDisplayList: List<String>) {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, genreDisplayList
        )
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View, position: Int, id: Long
            ) {
                if (position == 0 && genreDisplayList[position] == "All") {
                    galleryAdapter?.initList()
                } else {
                    galleryAdapter?.updateList(
                        Utils.generateNewGalleryList(
                            genreDisplayList[position], SharedPreferenceUtils.getGenreArrayList(
                                this@GalleryActivity,
                                SharedPreferenceUtils.key_id_genre_list
                            ), galleryAdapter?.getOriList()
                        )
                    )
                }
                Toast.makeText(
                    this@GalleryActivity,
                    "" + genreDisplayList[position], Toast.LENGTH_SHORT
                ).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
    }
}