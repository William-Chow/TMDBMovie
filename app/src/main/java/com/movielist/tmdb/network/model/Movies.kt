package com.movielist.tmdb.network.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Movies {

    var page: Int? = null

    var results: List<Movie>? = null

    var total_pages: Int? = null

    var total_results: Int? = null
}