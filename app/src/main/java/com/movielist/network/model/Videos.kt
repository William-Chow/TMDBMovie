package com.movielist.network.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
class Videos {

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("results")
    var results: List<Video>? = null
}