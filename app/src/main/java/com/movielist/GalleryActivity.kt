package com.movielist

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.movielist.adapter.GalleryAdapter
import com.movielist.network.MovieApi
import com.movielist.network.RetrofitClient
import com.movielist.network.model.Movies
import com.movielist.util.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GalleryActivity : AppCompatActivity() {

    lateinit var rvGalleryView: RecyclerView
    private lateinit var btnSwipe: Button

    private lateinit var movieAPI: MovieApi

    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        rvGalleryView = findViewById(R.id.rvGalleryView)
        btnSwipe = findViewById(R.id.btnSwipe)

        btnSwipe.setOnClickListener {
            Utils.intent(this@GalleryActivity, MainActivity::class.java)
        }

        movieAPI = RetrofitClient.getClient().create(MovieApi::class.java)
        if (Utils.checkInternetConnection(this@GalleryActivity)) {
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
                        val galleryAdapter = GalleryAdapter(results, this@GalleryActivity)
                        rvGalleryView.adapter = galleryAdapter
                    }
                }
            }

            override fun onFailure(call: Call<Movies>, t: Throwable) {
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

    override fun onBackPressed() {
        if (backPressedTime + 3000 > System.currentTimeMillis()) {
            onBackPressedDispatcher.onBackPressed()
            finish()
        } else {
            Toast.makeText(this, "Press back again to leave the app.", Toast.LENGTH_LONG).show()
        }
        backPressedTime = System.currentTimeMillis()
    }
}