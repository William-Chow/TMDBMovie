package com.movielist.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.movielist.MovieActivity
import com.movielist.R
import com.movielist.network.model.Movie
import com.movielist.util.Utils

class ViewPagerFragment(var movie: Movie) : Fragment() {

    private lateinit var rlContent: RelativeLayout
    private lateinit var ivMovieImage: ImageView
    private lateinit var tvTitle: TextView
    private var requestOptions: RequestOptions? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_movie, container, false) as ViewGroup
        rlContent = view.findViewById(R.id.rlContent)
        ivMovieImage = view.findViewById(R.id.ivMovieImage)
        tvTitle = view.findViewById(R.id.tvTitle)
        requestOptions = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Glide.with(requireContext()).apply { requestOptions }
            .load(Utils.imageURL + movie.poster_path).placeholder(R.drawable.ic_no_exist).dontTransform().fitCenter()
            .dontAnimate().into(ivMovieImage)
        tvTitle.text = movie.title
        rlContent.setOnClickListener {
            Utils.intent(requireContext(), movie.id, MovieActivity::class.java)
        }
    }
}