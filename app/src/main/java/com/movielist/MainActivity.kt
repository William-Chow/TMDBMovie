package com.movielist

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.movielist.adapter.MovieSwipeViewAdapter
import com.movielist.network.MovieApi
import com.movielist.network.RetrofitClient
import com.movielist.network.model.Movies
import com.movielist.util.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var btnGallery: Button

    private lateinit var movieAPI: MovieApi

    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.vpMovieView)
        btnGallery = findViewById(R.id.btnGallery)

        btnGallery.setOnClickListener {
            Utils.intent(this@MainActivity, GalleryActivity::class.java)
        }
        movieAPI = RetrofitClient.getClient().create(MovieApi::class.java)
        if (Utils.checkInternetConnection(this@MainActivity)) {
            getMovies()
        } else {
            Toast.makeText(
                this@MainActivity,
                this@MainActivity.getString(R.string.no_internet_connection),
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
                        val adapter = MovieSwipeViewAdapter(this@MainActivity, results)
                        viewPager.adapter = adapter
                    }
                }
            }

            override fun onFailure(call: Call<Movies>, t: Throwable) {
                Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT).show()
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
            Utils.intent(this@MainActivity, SearchActivity::class.java)
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
}