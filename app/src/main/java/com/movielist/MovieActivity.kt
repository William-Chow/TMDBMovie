package com.movielist

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.movielist.network.MovieApi
import com.movielist.network.RetrofitClient
import com.movielist.network.model.Movie
import com.movielist.network.model.Videos
import com.movielist.util.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MovieActivity : AppCompatActivity() {

    lateinit var ivMovieImage: ImageView
    lateinit var tvTitle: TextView
    lateinit var tvYear: TextView
    lateinit var tvGenre: TextView
    lateinit var tvOverview: TextView
    lateinit var btnBackButton: Button

    lateinit var movieAPI: MovieApi
    private var requestOptions: RequestOptions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie)

        ivMovieImage = findViewById(R.id.ivMovieImage)
        tvTitle = findViewById(R.id.tvTitle)
        tvYear = findViewById(R.id.tvYear)
        tvGenre = findViewById(R.id.tvGenre)
        tvOverview = findViewById(R.id.tvOverview)
        btnBackButton = findViewById(R.id.btnGoBack)
        btnBackButton.setOnClickListener {
            finish()
        }
        requestOptions =
            RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
        movieAPI = RetrofitClient.getClient().create(MovieApi::class.java)

        val movieID = intent?.getIntExtra("movie", 0)
        if (movieID != null && movieID != 0) {
            if (Utils.checkInternetConnection(this@MovieActivity)) {
                getMovie(movieID)
            } else {
                Toast.makeText(
                    this@MovieActivity,
                    this@MovieActivity.getString(R.string.no_internet_connection),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getMovie(movieID: Int) {
        movieAPI.getMovie(movieID, RetrofitClient.API_KEY).enqueue(object : Callback<Movie> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call<Movie>, response: Response<Movie>) {
                // val statusCode = response.code()
                val movie: Movie? = response.body()
                if (null != movie) {
                    Glide.with(this@MovieActivity).apply { requestOptions }
                        .load(Utils.imageURL + movie.poster_path)
                        .placeholder(R.drawable.ic_no_exist).dontAnimate().into(ivMovieImage)
                    tvTitle.text = movie.title
                    tvYear.text =
                        movie.release_date + " (" + movie.release_date?.let { Utils.getYear(it) } + ")"
                    tvGenre.text = Utils.getGenres(movie.genres)
                    tvOverview.text = movie.overview?.ifEmpty { "-" }
                }
            }

            override fun onFailure(call: Call<Movie>, t: Throwable) {
                Toast.makeText(this@MovieActivity, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getVideo(movieID: Int) {
        movieAPI.getVideo(movieID, RetrofitClient.API_KEY).enqueue(object : Callback<Videos> {
            override fun onResponse(call: Call<Videos>, response: Response<Videos>) {
                // val statusCode = response.code()
                val videos: Videos? = response.body()
                val results = videos?.results
                // Video View no support youtube play link
            }

            override fun onFailure(call: Call<Videos>, t: Throwable) {
                Toast.makeText(this@MovieActivity, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
}