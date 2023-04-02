package com.movielist.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.movielist.MovieActivity
import com.movielist.R
import com.movielist.network.model.Movie
import com.movielist.util.Utils

class GalleryAdapter(
    private var movieList: List<Movie>,
    private val context: Context
) : RecyclerView.Adapter<GalleryAdapter.CourseViewHolder>() {

    var saveList: List<Movie> = emptyList()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CourseViewHolder {

        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.item_gallery,
            parent, false
        )
        return CourseViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.title.text = movieList[position].title
        Glide.with(context).load(Utils.imageURL + movieList[position].poster_path)
            .placeholder(R.drawable.ic_no_exist).dontAnimate().into(holder.imageIcon)

        holder.llContent.setOnClickListener {
            Utils.intent(context, movieList[position].id, MovieActivity::class.java)
        }
    }

    override fun getItemCount(): Int {
        return movieList.size
    }

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val llContent: LinearLayout = itemView.findViewById(R.id.llContent)
        val title: TextView = itemView.findViewById(R.id.ivMovieTitle)
        val imageIcon: ImageView = itemView.findViewById(R.id.ivMovieIcon)
    }

    fun getOriList(): List<Movie> {
        return this.saveList
    }

    fun saveList(results: List<Movie>) {
        this.saveList = results
    }

    @SuppressLint("NotifyDataSetChanged")
    fun initList() {
        movieList = this.saveList
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(generateNewGalleryList: List<Movie>) {
        movieList = generateNewGalleryList
        notifyDataSetChanged()
    }
}