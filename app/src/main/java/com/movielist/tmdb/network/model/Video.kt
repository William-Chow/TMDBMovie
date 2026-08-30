package com.movielist.tmdb.network.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Video {
    var iso_639_1: String? = null

    var iso_3166_1: String? = null

    var name: String? = null

    var key: String? = null

    var site: String? = null

    var size: Int? = null

    var type: String? = null

    var official: Boolean? = null

    var published_at: String? = null

    var id: String? = null
}