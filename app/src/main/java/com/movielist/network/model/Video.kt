package com.movielist.network.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
class Video {
    @SerializedName("iso_639_1")
    var iso_639_1: String? = null

    @SerializedName("iso_3166_1")
    var iso_3166_1: String? = null

    @SerializedName("name")
    var name: String? = null

    @SerializedName("key")
    var key: String? = null

    @SerializedName("site")
    var site: String? = null

    @SerializedName("size")
    var size: Int? = null

    @SerializedName("type")
    var type: String? = null

    @SerializedName("official")
    var official: Boolean? = null

    @SerializedName("published_at")
    var published_at: String? = null

    @SerializedName("id")
    var id: String? = null
}