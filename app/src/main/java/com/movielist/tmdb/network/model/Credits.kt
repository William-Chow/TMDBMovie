package com.movielist.tmdb.network.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Credits {

    var id: Int? = null

    var cast: List<Cast>? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Cast {

    var id: Int? = null

    var name: String? = null

    var character: String? = null

    var profile_path: String? = null

    var order: Int? = null
}
