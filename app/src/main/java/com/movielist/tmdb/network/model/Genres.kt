package com.movielist.tmdb.network.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
class Genres {
    @SerializedName("genres")
    var genres: List<Genre>? = null
}