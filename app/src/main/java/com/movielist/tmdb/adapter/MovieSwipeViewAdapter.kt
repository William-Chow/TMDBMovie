package com.movielist.tmdb.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.movielist.tmdb.network.model.Movie
import com.movielist.tmdb.ui.ViewPagerFragment


class MovieSwipeViewAdapter(fragmentActivity: FragmentActivity, private var movies: List<Movie>) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return movies.size
    }

    override fun createFragment(position: Int): Fragment {
        val movie: Movie = movies[position]
        return ViewPagerFragment(movie)
    }

}