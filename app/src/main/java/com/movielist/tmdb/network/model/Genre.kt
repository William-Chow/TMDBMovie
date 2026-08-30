package com.movielist.tmdb.network.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Genre {

    var id: Int? = null

    var name: String? = null
}