package com.example.streambox

data class Movie(
    val title: String,
    val category: String,
    val description: String,
    val videoUrl: String,
    val posterUrl: String,
    val id: String = "",
    val tmdbId: Int = 0,
    val mediaType: String = "movie"
)
