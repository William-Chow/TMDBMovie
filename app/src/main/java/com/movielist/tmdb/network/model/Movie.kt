package com.movielist.tmdb.network.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Movie {

    var adult: Boolean? = null

    var backdrop_path: String? = null

    var belongs_to_collection: BelongToCollection? = null

    var budget: Long? = null

    var genres: List<Genre>? = null

    var homepage: String? = null

    var genre_ids: List<Int>? = null

    var id: Int? = null

    var imdb_id: String? = null

    var original_language: String? = null

    var original_title: String? = null

    var overview: String? = null

    var popularity: Double? = null

    var poster_path: String? = null

    var production_companies: List<ProductionCompanies>? = null

    var production_countries: List<ProductionCountries>? = null

    var release_date: String? = null

    var revenue: Long? = null

    var runtime: Int? = null

    var spoken_languages: List<SpokenLanguages>? = null

    var status: String? = null

    var tagline: String? = null

    var title: String? = null

    var video: Boolean? = null

    var vote_average: Double? = null

    var vote_count: Int? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class BelongToCollection {
    var id: Int? = null

    var name: String? = null

    var poster_path: String? = null

    var backdrop_path: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ProductionCompanies {

    var id: Int? = null

    var logo_path: String? = null

    var name: String? = null

    var origin_country: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ProductionCountries {

    var iso_3166_1: String? = null

    var name: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class SpokenLanguages {
    var english_name: String? = null

    var iso_639_1: String? = null

    var name: String? = null
}