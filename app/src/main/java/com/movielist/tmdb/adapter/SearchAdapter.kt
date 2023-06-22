package com.movielist.tmdb.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.MovieActivity
import com.movielist.tmdb.R
import com.movielist.tmdb.util.Utils

class SearchAdapter(var context: Context, private var movieList: List<Movie>) :
    RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(context).load(Utils.imageURL + movieList[position].poster_path)
            .placeholder(R.drawable.ic_no_exist).dontAnimate().into(holder.ivMovieIcon)
        holder.tvMovieName.text = movieList[position].title
        holder.llContent.setOnClickListener {
            Utils.intent(context, movieList[position].id, MovieActivity::class.java)
        }
        holder.tvMovieOverview.text = movieList[position].overview
    }

    override fun getItemCount(): Int {
        return movieList.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val llContent: LinearLayout = itemView.findViewById(R.id.llContent)
        val ivMovieIcon: ImageView = itemView.findViewById(R.id.ivMovieIcon)
        val tvMovieName: TextView = itemView.findViewById(R.id.tvMovieName)
        val tvMovieOverview: TextView = itemView.findViewById(R.id.tvMovieOverview)
    }

    fun updateList(_movieList: List<Movie>) {
        this.movieList = _movieList
    }
}