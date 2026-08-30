package com.movielist.tmdb.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/** The slice of a movie worth keeping on disk for the favourites grid. */
@JsonIgnoreProperties(ignoreUnknown = true)
class FavoriteMovie {

    var id: Int? = null

    var title: String? = null

    var poster_path: String? = null
}
