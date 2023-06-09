package com.movielist.tmdb

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.movielist.tmdb.adapter.SearchAdapter
import com.movielist.tmdb.network.MovieApi
import com.movielist.tmdb.network.RetrofitClient
import com.movielist.tmdb.network.model.Movies
import com.movielist.tmdb.util.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : AppCompatActivity() {

    private lateinit var mAdView : AdView

    private lateinit var ivEmpty: ImageView
    private lateinit var searchRV: RecyclerView
    private lateinit var btnGoBack: Button

    lateinit var searchAdapter: SearchAdapter

    private lateinit var movieAPI: MovieApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        initAdmob()
        admobBanner()

        ivEmpty = findViewById(R.id.ivEmpty)
        ivEmpty.visibility = View.VISIBLE
        searchRV = findViewById(R.id.rvSearch)
        btnGoBack = findViewById(R.id.btnGoBack)

        btnGoBack.setOnClickListener {
            finish()
        }
        movieAPI = RetrofitClient.getClient().create(MovieApi::class.java)
        searchRV.layoutManager = LinearLayoutManager(this@SearchActivity)
        searchAdapter = SearchAdapter(this@SearchActivity, emptyList())
        searchRV.adapter = searchAdapter
    }

    private fun initAdmob(){
        MobileAds.initialize(this) { }
    }

    private fun admobBanner(){
        mAdView = findViewById(R.id.adView)
        mAdView.loadAd(AdRequest.Builder().build())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.search_menu, menu)
        val searchItem: MenuItem = menu!!.findItem(R.id.searchView)
        val searchView: SearchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onQueryTextChange(msg: String): Boolean {
                if (Utils.checkInternetConnection(this@SearchActivity)) {
                    // Retrofit
                    if (msg.length > 2) {
                        getSearch(msg)
                    } else {
                        ivEmpty.visibility = View.VISIBLE
                        searchAdapter.updateList(emptyList())
                        searchAdapter.notifyDataSetChanged()
                    }
                } else {
                    Toast.makeText(
                        this@SearchActivity,
                        this@SearchActivity.getString(R.string.no_internet_connection),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return false
            }
        })
        return true
    }

    fun getSearch(msg: String) {
        movieAPI.getSearch(RetrofitClient.API_KEY, msg, 1).enqueue(object : Callback<Movies> {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call<Movies>, response: Response<Movies>) {
                //val statusCode = response.code()
                val movies: Movies? = response.body()
                if (movies?.results != null) {
                    val results = movies.results
                    if (results?.size!! > 0) {
                        ivEmpty.visibility = View.GONE
                        searchAdapter.updateList(results)
                        searchAdapter.notifyDataSetChanged()
                    } else {
                        ivEmpty.visibility = View.VISIBLE
                        searchAdapter.updateList(emptyList())
                        searchAdapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onFailure(call: Call<Movies>, t: Throwable) {
                Toast.makeText(this@SearchActivity, t.message, Toast.LENGTH_SHORT).show()
            }

        })
    }
}