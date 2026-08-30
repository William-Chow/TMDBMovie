package com.movielist.tmdb.network.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Genres {
    var genres: List<Genre>? = null
}